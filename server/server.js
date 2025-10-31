import express from 'express'
import cors from 'cors'
import dotenv from 'dotenv'
import Stripe from 'stripe'

dotenv.config()

const app = express()
app.use(express.json())
app.use(cors({ origin: '*'}))

const stripeSecret = process.env.STRIPE_SECRET_KEY
if (!stripeSecret) {
  console.error('Missing STRIPE_SECRET_KEY in environment')
  process.exit(1)
}
const stripe = new Stripe(stripeSecret)

// Create PaymentIntent for card payments
app.post('/create-payment-intent', async (req, res) => {
  try {
    const { amount, currency = 'usd', paymentMethodType = 'card' } = req.body || {}
    if (!amount || typeof amount !== 'number') {
      return res.status(400).json({ error: 'Invalid amount' })
    }

    const intent = await stripe.paymentIntents.create({
      amount,
      currency,
      automatic_payment_methods: { enabled: true }
    })

    return res.json({ clientSecret: intent.client_secret, id: intent.id })
  } catch (err) {
    console.error('create-payment-intent error:', err)
    return res.status(500).json({ error: err.message })
  }
})

// Optional premium status endpoint (stub)
app.get('/premium-status', async (req, res) => {
  // In production, look up the user by phone or account and return true/false
  return res.json({ premium: true })
})

const port = process.env.PORT || 4242
app.listen(port, () => {
  console.log(`Stripe server listening on http://localhost:${port}`)
})