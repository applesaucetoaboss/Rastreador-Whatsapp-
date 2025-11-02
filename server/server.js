
import express from 'express'
import cors from 'cors'
import dotenv from 'dotenv'
import Stripe from 'stripe'
import morgan from 'morgan'

dotenv.config()

// Startup validation for required env vars
const requiredEnv = ['STRIPE_SECRET_KEY', 'STRIPE_PRICE_MXN_180_MONTHLY', 'STRIPE_WEBHOOK_SECRET'];
const missingEnvVars = requiredEnv.filter(key => !process.env[key]);

if (missingEnvVars.length > 0) {
  console.error('❌ CRITICAL: Missing required environment variables:');
  missingEnvVars.forEach(key => console.error(`   - ${key}`));
  console.error('\n📋 Required Stripe configuration:');
  console.error('   STRIPE_SECRET_KEY=sk_live_... (or sk_test_...)');
  console.error('   STRIPE_WEBHOOK_SECRET=whsec_...');
  console.error('   STRIPE_PRICE_MXN_180_MONTHLY=price_...');
  console.error('\n🔧 Configure these in your Render dashboard under Environment Variables');
  process.exit(1);
}

console.log('✅ All required environment variables configured');
console.log(`🚀 Starting server in ${process.env.NODE_ENV || 'development'} mode`);

const app = express()
app.use(cors({ 
  origin: [
    'http://localhost',
    'http://localhost:3000',
    'http://localhost:8080',
    'https://rastreador-whatsapp.onrender.com',
    'https://rastreador-whatsapp-server.onrender.com',
    /^https:\/\/.*\.onrender\.com$/
  ],
  credentials: true
}));
app.use(morgan('combined'))

const stripeSecret = process.env.STRIPE_SECRET_KEY;
const stripe = new Stripe(stripeSecret)

import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'
const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)
// Resolve project paths correctly on Render
const projectRoot = path.join(__dirname, '..')
const publicDir = path.join(projectRoot, 'public')
const dataDir = path.join(projectRoot, 'data')
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

// Webhook (raw body)
app.post('/webhook', express.raw({ type: 'application/json' }), async (req, res) => {
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

  if (event.type === 'invoice.payment_succeeded') {
    try {
      const invoice = event.data.object
      let phone = null
      if (invoice.payment_intent) {
        const pi = await stripe.paymentIntents.retrieve(invoice.payment_intent)
        phone = pi.metadata && pi.metadata.phone
      }
      if (!phone && invoice.customer) {
        const customer = await stripe.customers.retrieve(invoice.customer)
        phone = customer.phone || (customer.metadata && customer.metadata.phone)
      }
      if (phone) {
        const map = readPremium()
        map[phone] = true
        writePremium(map)
        console.log('Marked premium from subscription for phone', phone)
      } else {
        console.warn('invoice.payment_succeeded received but no phone found')
      }
    } catch (e) {
      console.error('Error handling invoice.payment_succeeded', e)
    }
  }
  res.json({ received: true })
})

// JSON parser after webhook
app.use(express.json())

// PaymentIntent
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

    console.log('Created PaymentIntent', { id: intent.id, amount, currency })
    return res.json({ clientSecret: intent.client_secret, id: intent.id })
  } catch (err) {
    console.error('create-payment-intent error:', { message: err.message, type: err.type, code: err.code })
    return res.status(500).json({ error: err.message })
  }
})

// Subscription (MXN 180)
app.post('/create-subscription', async (req, res) => {
  try {
    const { phone } = req.body || {}
    const priceId = process.env.STRIPE_PRICE_MXN_180_MONTHLY
    if (!priceId) {
      return res.status(500).json({ error: 'Missing STRIPE_PRICE_MXN_180_MONTHLY' })
    }

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
    console.log('Created Subscription', { id: subscription.id, customer: customer.id })
    return res.json({ clientSecret: pi.client_secret, subscriptionId: subscription.id })
  } catch (err) {
    console.error('create-subscription error:', { message: err.message, type: err.type, code: err.code })
    return res.status(500).json({ error: err.message })
  }
})

// Serve static site from /public at repository root
app.use(express.static(publicDir))

// Root route → serve public/index.html
app.get('/', (req, res) => {
  try {
    return res.sendFile(path.join(publicDir, 'index.html'))
  } catch (e) {
    console.error('Error serving index.html', e)
    return res.status(500).send('Server error')
  }
})

// Health
app.get('/health', (req, res) => {
  res.json({ ok: true })
})

// Premium status
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
