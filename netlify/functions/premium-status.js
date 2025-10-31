// Netlify Function: premium-status
// Simple demo endpoint returning premium status for a phone number.
// In production, replace this with real persistence (DB / Stripe webhook checks).

exports.handler = async (event) => {
  const headers = {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Headers': 'Content-Type',
    'Access-Control-Allow-Methods': 'GET, OPTIONS',
  };

  // Handle CORS preflight
  if (event.httpMethod === 'OPTIONS') {
    return { statusCode: 200, headers, body: '' };
  }

  if (event.httpMethod !== 'GET') {
    return {
      statusCode: 405,
      headers,
      body: JSON.stringify({ error: 'Method Not Allowed' }),
    };
  }

  try {
    const params = event.queryStringParameters || {};
    const phone = (params.phone || '').trim();

    if (!phone) {
      return {
        statusCode: 400,
        headers,
        body: JSON.stringify({ error: 'Missing phone parameter' }),
      };
    }

    // Demo logic: mark premium as true to unblock app usage.
    // Swap to real logic as needed.
    const premium = true;

    return {
      statusCode: 200,
      headers,
      body: JSON.stringify({ premium }),
    };
  } catch (err) {
    return {
      statusCode: 500,
      headers,
      body: JSON.stringify({ error: 'Server error', details: String(err) }),
    };
  }
};