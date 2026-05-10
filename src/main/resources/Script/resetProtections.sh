sudo bash << 'EOF'
# Variabelen
MAIL_BASE="/home/mail"
MAIL_USER="vmail"
MAIL_GROUP="vmail"

echo "=== Herstel permissies voor $MAIL_BASE ==="

# 1. Zet juiste eigenaar op alles
chown -R $MAIL_USER:$MAIL_GROUP "$MAIL_BASE"
echo "✓ Eigenaren hersteld naar $MAIL_USER:$MAIL_GROUP"

# 2. Directories: 750 (drwxr-x---) of 755 voor public
find "$MAIL_BASE" -type d -exec chmod 750 {} \;
echo "✓ Directory permissies: 750"

# 3. Bestanden: 640 (rw-r-----)
find "$MAIL_BASE" -type f -exec chmod 640 {} \;
echo "✓ Bestandspermissies: 640"

# 4. Speciale bestanden die uitvoerbaar moeten zijn (indien van toepassing)
# Dovecot sieve scripts etc.
find "$MAIL_BASE" -name "*.sieve" -exec chmod 750 {} \; 2>/dev/null

# 5. Verwijder ACLs (Access Control Lists)
find "$MAIL_BASE" -exec setfacl -b {} \; 2>/dev/null
echo "✓ ACLs verwijderd"

# 6. Zet juiste permissies voor Maildir specifiek
find "$MAIL_BASE" -type d -name "cur" -exec chmod 700 {} \;
find "$MAIL_BASE" -type d -name "new" -exec chmod 700 {} \;
find "$MAIL_BASE" -type d -name "tmp" -exec chmod 700 {} \;
echo "✓ Maildir speciale mappen: 700"

echo "=== Herstel voltooid ==="
EOF