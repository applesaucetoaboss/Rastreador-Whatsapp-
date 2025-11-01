import express from 'express'
import cors from 'cors'
import dotenv from 'dotenv'
import Stripe from 'stripe'
import morgan from 'morgan'

dotenv.config()

// Startup validation for required env vars
const requiredEnv = ['STRIPE_SECRET_KEY', 'STRIPE_PRICE_MXN_180_MONTHLY', 'STRIPE_WEBHOOK_SECRET'];
requiredEnv.forEach(key => {
  if (!process.env[key]) {
    console.error(`Missing required env var: ${key}`);
    process.exit(1);
  }
});

const app = express()
app.use(cors({ origin: ['http://localhost', 'https://your-production-domain.com'] })); // Tighten CORS for security
// Basic request logging for observability on Render
app.use(morgan('combined'))

const stripeSecret = process.env.STRIPE_SECRET_KEY;
const stripe = new Stripe(stripeSecret)

// Simple file-backed premium store for demo/prod-lite
import fs from 'fs'
import path from 'path'
const dataDir = path.join(process.cwd(), 'data')
const premiumFile = path.join(dataDir, 'premium.json')
function readPremium() {
  try {
    if (!fs.existsSync(dataDir)) fs.mkdirSync(dataDir)
    if (!fs.existsSync(premiumFile)) fs.writeFileSync(premiumFile, JSON.stringify({}), 'utf8')
    return JSON.parse(fs.readFileSync(premiumFile, 'utf8'))
  } catch (e) {
    console.error('readPremium error', e)
    return {}
  }
}
function writePremium(map) {
  try {
    fs.writeFileSync(premiumFile, JSON.stringify(map, null, 2), 'utf8')
  } catch (e) {
    console.error('writePremium error', e)
  }
}

// Webhook to mark premium after successful payment (use raw body before JSON parser)
app.post('/webhook', express.raw({ type: 'application/json' }), (req, res) => {
  const sig = req.headers['stripe-signature']
  const webhookSecret = process.env.STRIPE_WEBHOOK_SECRET
  let event
  try {
    if (webhookSecret) {
      event = stripe.webhooks.constructEvent(req.body, sig, webhookSecret)
    } else {
      event = JSON.parse(req.body)
    }
  } catch (err) {
    console.error('Webhook signature verification failed:', err.message)
    return res.status(400).send(`Webhook Error: ${err.message}`)
  }

  if (event.type === 'payment_intent.succeeded') {
    const intent = event.data.object
    const phone = intent.metadata && intent.metadata.phone
    if (phone) {
      const map = readPremium()
      map[phone] = true
      writePremium(map)
      console.log('Marked premium for phone', phone)
    }
  }
  res.json({ received: true })
})

// JSON parser for normal API routes (must come AFTER webhook raw route)
app.use(express.json())

// Create PaymentIntent for card payments
app.post('/create-payment-intent', async (req, res) => {
  try {
    const { amount, currency = 'usd', paymentMethodType = 'card', phone } = req.body || {}
    if (!amount || typeof amount !== 'number') {
      return res.status(400).json({ error: 'Invalid amount' })
    }

    const intent = await stripe.paymentIntents.create({
      amount,
      currency,
      automatic_payment_methods: { enabled: true },
      metadata: phone ? { phone } : undefined
    })

    return res.json({ clientSecret: intent.client_secret, id: intent.id })
  } catch (err) {
    console.error('create-payment-intent error:', err)
    return res.status(500).json({ error: err.message })
  }
})

// Create Subscription (MXN 180 monthly) and return first invoice PaymentIntent client secret
app.post('/create-subscription', async (req, res) => {
  try {
    const { phone } = req.body || {}
    const priceId = process.env.STRIPE_PRICE_MXN_180_MONTHLY
    if (!priceId) {
      return res.status(500).json({ error: 'Missing STRIPE_PRICE_MXN_180_MONTHLY' })
    }

    // Create a customer; in production, deduplicate and attach email if available
    const customer = await stripe.customers.create({
      phone,
      metadata: phone ? { phone } : undefined
    })

    const subscription = await stripe.subscriptions.create({
      customer: customer.id,
      items: [{ price: priceId }],
      payment_behavior: 'default_incomplete',
      expand: ['latest_invoice.payment_intent'],
      payment_settings: { save_default_payment_method: 'on_subscription' },
      payment_intent_data: {
        metadata: phone ? { phone } : undefined
      }
    })

    const pi = subscription.latest_invoice.payment_intent
    return res.json({ clientSecret: pi.client_secret, subscriptionId: subscription.id })
  } catch (err) {
    console.error('create-subscription error:', err)
    return res.status(500).json({ error: err.message })
  }
})

// Serve static files from /public to mirror Netlify publish directory
app.use(express.static(path.join(process.cwd(), 'public')))

// Simple health check endpoint for Render
app.get('/health', (req, res) => {
  res.json({ ok: true })
})

// Premium status endpoint
app.get('/premium-status', async (req, res) => {
  const phone = (req.query.phone || '').toString()
  if (!phone) return res.status(400).json({ error: 'Missing phone' })
  const map = readPremium()
  return res.json({ premium: !!map[phone] })
})

const port = process.env.PORT || 4242
app.listen(port, () => {
  console.log(`Stripe server listening on http://localhost:${port}`)
})
