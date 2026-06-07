const dns = require('dns');

dns.resolveCname('vigeqwzqasblsnetbprm.supabase.co', (err, addresses) => {
  if (err) {
    console.error('Cname resolution failed:', err);
  } else {
    console.log('Cname addresses:', addresses);
  }
});

dns.resolveAny('vigeqwzqasblsnetbprm.supabase.co', (err, addresses) => {
  if (err) {
    console.error('Any resolution failed:', err);
  } else {
    console.log('Any addresses:', addresses);
  }
});
