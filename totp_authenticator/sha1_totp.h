#ifndef SHA1_TOTP_H
#define SHA1_TOTP_H

#include <Arduino.h>

// Public API — only this function is needed by the main sketch
void hmac_sha1(const uint8_t *key, uint8_t keylen,
               const uint8_t *msg, uint8_t msglen,
               uint8_t *digest);

#endif
