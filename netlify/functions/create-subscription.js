const Stripe = require('stripe')

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'Content-Type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS'
}

exports.handler = async (event) => {
  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers: corsHeaders, body: '' }
  }

  if (event.httpMethod !== 'POST') {
    return { statusCode: 405, headers: corsHeaders, body: JSON.stringify({ error: 'Method Not Allowed' }) }
  }

  try {
    const stripeSecret = process.env.STRIPE_SECRET_KEY
    const priceId = process.env.STRIPE_PRICE_MXN_180_MONTHLY
    if (!stripeSecret || !priceId) {
      return { statusCode: 500, headers: corsHeaders, body: JSON.stringify({ error: 'Missing STRIPE secrets' }) }
    }
    const stripe = new Stripe(stripeSecret)

    const payload = JSON.parse(event.body || '{}')
    const { phone } = payload

    const customer = await stripe.customers.create({ phone, metadata: phone ? { phone } : undefined })
    const subscription = await stripe.subscriptions.create({
      customer: customer.id,
      items: [{ price: priceId }],
      payment_behavior: 'default_incomplete',
      expand: ['latest_invoice.payment_intent'],
      payment_settings: { save_default_payment_method: 'on_subscription' }
    })

    const pi = subscription.latest_invoice.payment_intent
    return {
      statusCode: 200,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientSecret: pi.client_secret, subscriptionId: subscription.id, paymentIntentId: pi.id })
    }
  } catch (err) {
    return {
      statusCode: 500,
      headers: corsHeaders,
      body: JSON.stringify({ error: err.message })
    }
  }
}
