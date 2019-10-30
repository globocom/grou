# GROU

## Requirements

* docker
* docker-compose
*  python-openstackclient (pip install -r openstackclient-requirements.txt)
* curl

## start all necessary services
```
docker-compose up -d
```
## OpenStack Keystone: Create domain, project and user
```
while ! curl --connect-timeout 1 http://127.0.0.1:5000 > /dev/null 2> /dev/null; do echo "Waiting 5000/tcp... (press CTRL+C to cancel)"; sleep 5; done
cat <<EOF | docker exec -i k1.local bash -
source ~/openrc
openstack domain create grou > /dev/null
openstack project create grou --domain grou > /dev/null
openstack project create grouadmins --domain grou > /dev/null
openstack user create --domain grou --project grou --password grou grouadmin > /dev/null
openstack role add --domain grou --project-domain grou-grou --user grouadmin admin > /dev/null
openstack role add --project grou --project-domain grou --user grouadmin admin > /dev/null
openstack role add --project grouadmins --project-domain grou --user grouadmin admin > /dev/null
openstack role assignment list > /dev/null
EOF
```
# Define necessary var env
```
export OS_USER_DOMAIN_NAME=grou
export OS_IMAGE_API_VERSION=2
export OS_PROJECT_NAME=grou
export OS_IDENTITY_API_VERSION=3
export OS_PASSWORD=grou
export OS_AUTH_URL=http://k1:5000/v3
export OS_USERNAME=grouadmin
export OS_PROJECT_DOMAIN_NAME=grou

export REQUESTS_LIMIT=10
```
# If use email notification
```
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT=587
export MAIL_TLS="true"
export MAIL_USER="no-replay+anemailvalid@gmail.com"
export MAIL_PASS="gmailpass"
```
## Build and run GROU
```
nohup make grou run > /tmp/out < /dev/null 2>&1 &
while ! curl --connect-timeout 1 http://127.0.0.1:8080 > /dev/null 2> /dev/null; do echo "Waiting 8080/tcp... (press CTRL+C to cancel)"; sleep 5; done
```
## Get Keystone Token
```
export TOKEN="$(curl --silent -u grouadmin:grou 127.0.0.1:8080/token/grou | jq -r .token)"
```
# OR using 
```
export TOKEN="$(openstack token issue -f value -c id)"
```
## Create new test
```
curl -v -H'content-type:application/json' -H"x-auth-token:${TOKEN}" -d'
{
  "name":"'$RANDOM'",
  "durationTimeMillis":10000,
  "project":"grou",
  "tags":["dsfdsfdsfds", "sdfsdfds", "23312 fsdfds", "d3f434", "rsdfsd", "fdsdfsdfsd"],
  "notify":[ "mailto:root@localhost.localdomain", "http://mywebhook.localhost.localdomain" ],
  "properties": {
    "requests": [
      {
        "order": 1,
        "uri": "https://www.bing.com"
      },
      {
        "order": 2,
        "uri": "https://httpbin.org/post",
        "method": "POST",
        "headers": { \"content-type\": \"application/json\" },
        "body": "{ \"id\": \"test\" }"
      },
      {
        "order": 3,
        "uri": "https://httpbin.org/post",
        "method": "POST",
        "headers": { "content-type": "application/json" },
        "body": "{ \"id\": \"test\" }",
        "auth": [
          { "credentials": "admin:password" },
          { "preemptive": "true" }
        ]
      },
      {
        "order": 4,
        "uri": "https://httpbin.org/post",
        "method": "POST",
        "headers": { "content-type": "application/x-www-form-urlencoded" },
        "body": "login=admin&pass=password",
        "saveCookies": true
      }
    ],
    "numConn": 1000,
    "parallelLoaders": 1,
    "followRedirect": true,
    "monitTargets": "zero://target1,zero://target2"
  }
}' http://127.0.0.1:8080/tests
```
## List all tests
```
curl -v -H'content-type:application/json' http://127.0.0.1:8080/tests
```
