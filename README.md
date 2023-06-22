HTTP/HTTPS抓包工具
==================

桌面版：https://github.com/wanghongenpin/network-proxy-flutter

[安卓证书](https://android.stackexchange.com/questions/237141/how-to-get-android-11-to-trust-a-user-root-ca-without-a-private-key/238859#238859) 

### CA证书生成

key的生成，这样是生成RSA密钥，openssl格式，2048位强度。ca.key是密钥文件名。

openssl genrsa -out ca.key 2048

key的转换，转换成netty支持私钥编码格式

openssl pkcs8 -topk8 -nocrypt -inform PEM -outform DER -in ca.key -out ca_private.der

openssl req -config openssl.cnf  -new -sha256 -x509 -days 3650  -key ca.key -out ca.crt

openssl.cnf文件内容如下：

```agsl
[ req ]
default_md          = sha256
default_days        = 825
distinguished_name  = subject
req_extensions      = req_ext
x509_extensions     = x509_ext
string_mask         = utf8only
prompt              = no
# The Subject DN can be formed using X501 or RFC 4514 (see RFC 4519 for a description).
#   Its sort of a mashup. For example, RFC 4514 does not provide emailAddress.

[ subject ]
countryName                 = CN
stateOrProvinceName         = BJ
localityName                = BeiJing
organizationName            = ProxyPin
OU                          = ProxyPin

# Use a friendly name here because it's presented to the user. The server's DNS
#   names are placed in Subject Alternate Names. Plus, DNS names here is deprecated
#   by both IETF and CA/Browser Forums. If you place a DNS name here, then you
#   must include the DNS name in the SAN too (otherwise, Chrome and others that
#   strictly follow the CA/Browser Baseline Requirements will fail).

commonName              =  ProxyPin CA

# Section x509_ext is used when generating a self-signed certificate. I.e., openssl req -x509 ...

[ x509_ext ]
subjectKeyIdentifier      = hash
authorityKeyIdentifier    = keyid:always,issuer

# You only need digitalSignature below. *If* you don't allow
#   RSA Key transport (i.e., you use ephemeral cipher suites), then
#   omit keyEncipherment because that's key transport.

basicConstraints        = critical, CA:TRUE
keyUsage            = critical, digitalSignature, keyEncipherment, cRLSign, keyCertSign
subjectAltName          = DNS:ProxyPin
extendedKeyUsage = serverAuth

# RFC 5280, Section 4.2.1.12 makes EKU optional
#   CA/Browser Baseline Requirements, Appendix (B)(3)(G) makes me confused
#   In either case, you probably only need serverAuth.

extendedKeyUsage    = TLS Web Server Authentication



# Section req_ext is used when generating a certificate signing request. I.e., openssl req ...

[ req_ext ]
subjectKeyIdentifier        = hash
basicConstraints        = CA:FALSE
keyUsage            = digitalSignature, keyEncipherment
subjectAltName          = DNS:ProxyPin
nsComment           = "OpenSSL Generated Certificate"
```
