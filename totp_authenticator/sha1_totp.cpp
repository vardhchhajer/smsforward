#include "sha1_totp.h"

// ═════════════════════════════════════════════════════════════
//  SHA-1 (minimal implementation for ATmega328P)
// ═════════════════════════════════════════════════════════════

#define SHA1_BLOCK_SIZE  64
#define SHA1_HASH_SIZE   20

typedef struct {
  uint32_t state[5];
  uint32_t count[2];
  uint8_t  buffer[SHA1_BLOCK_SIZE];
} SHA1_CTX;

static uint32_t rol32(uint32_t x, uint8_t n) {
  return (x << n) | (x >> (32 - n));
}

static void sha1_transform(uint32_t state[5], const uint8_t block[64]) {
  uint32_t w[16];
  uint32_t a, b, c, d, e;
  for (uint8_t i = 0; i < 16; i++)
    w[i] = ((uint32_t)block[i*4]<<24)|((uint32_t)block[i*4+1]<<16)
          |((uint32_t)block[i*4+2]<<8)|(uint32_t)block[i*4+3];
  a=state[0]; b=state[1]; c=state[2]; d=state[3]; e=state[4];
  for (uint8_t i = 0; i < 80; i++) {
    if (i >= 16) {
      uint32_t t = w[(i-3)&0xF]^w[(i-8)&0xF]^w[(i-14)&0xF]^w[i&0xF];
      w[i&0xF] = rol32(t,1);
    }
    uint32_t f, k;
    if      (i<20) { f=(b&c)|((~b)&d);          k=0x5A827999; }
    else if (i<40) { f=b^c^d;                    k=0x6ED9EBA1; }
    else if (i<60) { f=(b&c)|(b&d)|(c&d);        k=0x8F1BBCDC; }
    else           { f=b^c^d;                    k=0xCA62C1D6; }
    uint32_t temp = rol32(a,5)+f+e+k+w[i&0xF];
    e=d; d=c; c=rol32(b,30); b=a; a=temp;
  }
  state[0]+=a; state[1]+=b; state[2]+=c; state[3]+=d; state[4]+=e;
}

static void sha1_init(SHA1_CTX *ctx) {
  ctx->state[0]=0x67452301; ctx->state[1]=0xEFCDAB89;
  ctx->state[2]=0x98BADCFE; ctx->state[3]=0x10325476;
  ctx->state[4]=0xC3D2E1F0;
  ctx->count[0]=ctx->count[1]=0;
}

static void sha1_update(SHA1_CTX *ctx, const uint8_t *data, uint16_t len) {
  uint16_t i = 0;
  uint8_t j = (uint8_t)(ctx->count[0]>>3) & 63;
  if ((ctx->count[0] += (uint32_t)len<<3) < ((uint32_t)len<<3))
    ctx->count[1]++;
  ctx->count[1] += (uint32_t)len >> 29;
  if (j + len >= 64) {
    uint8_t fill = 64 - j;
    memcpy(&ctx->buffer[j], data, fill);
    sha1_transform(ctx->state, ctx->buffer);
    i = fill;
    for (; i + 63 < len; i += 64)
      sha1_transform(ctx->state, &data[i]);
    j = 0;
  }
  memcpy(&ctx->buffer[j], &data[i], len - i);
}

static void sha1_final(SHA1_CTX *ctx, uint8_t digest[20]) {
  uint8_t fc[8];
  for (uint8_t i=0;i<8;i++)
    fc[i]=(uint8_t)((ctx->count[(i>=4)?0:1]>>((3-(i&3))*8))&255);
  uint8_t pad=0x80; sha1_update(ctx,&pad,1);
  while (((ctx->count[0]>>3)&63)!=56) { pad=0; sha1_update(ctx,&pad,1); }
  sha1_update(ctx,fc,8);
  for (uint8_t i=0;i<20;i++)
    digest[i]=(uint8_t)((ctx->state[i>>2]>>((3-(i&3))*8))&255);
}

// ═════════════════════════════════════════════════════════════
//  HMAC-SHA1
// ═════════════════════════════════════════════════════════════

void hmac_sha1(const uint8_t *key, uint8_t klen,
               const uint8_t *msg, uint8_t mlen,
               uint8_t *digest) {
  uint8_t iPad[SHA1_BLOCK_SIZE], oPad[SHA1_BLOCK_SIZE];
  uint8_t kb[SHA1_BLOCK_SIZE];
  SHA1_CTX ctx;
  memset(kb,0,SHA1_BLOCK_SIZE);
  if (klen > SHA1_BLOCK_SIZE) {
    sha1_init(&ctx); sha1_update(&ctx,key,klen);
    sha1_final(&ctx,kb); klen=SHA1_HASH_SIZE;
  } else memcpy(kb,key,klen);
  for (uint8_t i=0;i<SHA1_BLOCK_SIZE;i++) {
    iPad[i]=kb[i]^0x36; oPad[i]=kb[i]^0x5C;
  }
  sha1_init(&ctx); sha1_update(&ctx,iPad,SHA1_BLOCK_SIZE);
  sha1_update(&ctx,msg,mlen);
  uint8_t inner[SHA1_HASH_SIZE]; sha1_final(&ctx,inner);
  sha1_init(&ctx); sha1_update(&ctx,oPad,SHA1_BLOCK_SIZE);
  sha1_update(&ctx,inner,SHA1_HASH_SIZE); sha1_final(&ctx,digest);
}
