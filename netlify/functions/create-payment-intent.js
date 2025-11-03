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
    if (!stripeSecret) {
      return { statusCode: 500, headers: corsHeaders, body: JSON.stringify({ error: 'Missing STRIPE_SECRET_KEY' }) }
    }
    const stripe = new Stripe(stripeSecret)

    const payload = JSON.parse(event.body || '{}')
    const { amount, currency = 'usd', phone } = payload

    if (!amount || typeof amount !== 'number') {
      return { statusCode: 400, headers: corsHeaders, body: JSON.stringify({ error: 'Invalid amount' }) }
    }

    const intent = await stripe.paymentIntents.create({
      amount,
      currency,
      automatic_payment_methods: { enabled: true },
      metadata: phone ? { phone } : undefined
    })

    return {
      statusCode: 200,
      headers: { ...corsHeaders, 'Content-Type': 'application/json' },
      body: JSON.stringify({ clientSecret: intent.client_secret, id: intent.id })
    }
  } catch (err) {
    return {
      statusCode: 500,
      headers: corsHeaders,
      body: JSON.stringify({ error: err.message })
    }
  }
}
