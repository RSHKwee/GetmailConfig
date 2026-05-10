#!/bin/bash
# Bepaal het adres op basis van de map
if [[ "$PWD" =~ /(corianne|rene|wilma|postmaster)/ ]]; then
    USER="${BASH_REMATCH[1]}"
    /usr/lib/dovecot/dovecot-lda -d "${USER}@kwee.cumail.nl"
else
    /usr/lib/dovecot/dovecot-lda -d "postmaster@kwee.cumail.nl"
fi