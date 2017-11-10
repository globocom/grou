# Setting Up

## Requirements

* docker
* docker-compose
* python-openstackclient (pip install -r openstackclient-requirements.txt)
* curl

## Build and start all

```
$ docker-compose up -d --build; sleep 5
$ while ! curl --connect-timeout 1 http://127.0.0.1:8080 > /dev/null 2> /dev/null; \
  do echo "Waiting 8080/tcp... (press CTRL+C to cancel)"; sleep 5; done
```

## OpenStack Keystone: Create domain, project and user

```
$ while ! curl --connect-timeout 1 http://127.0.0.1:5000 > /dev/null 2> /dev/null; \
  do echo "Waiting 5000/tcp... (press CTRL+C to cancel)"; sleep 5; done
$ cat <<EOF | docker exec -i k1.local bash -
source ~/openrc
openstack domain create grou > /dev/null
openstack project create grou --domain grou > /dev/null
openstack user create --domain grou --project grou --password grou grouadmin > /dev/null
openstack role add --domain grou --project-domain grou-grou --user grouadmin admin > /dev/null
openstack role add --project grou --project-domain grou --user grouadmin admin > /dev/null
openstack role assignment list > /dev/null
EOF
```

## Define necessary var envs

```
$ export OS_USER_DOMAIN_NAME=grou
$ export OS_IMAGE_API_VERSION=2
$ export OS_PROJECT_NAME=grou
$ export OS_IDENTITY_API_VERSION=3
$ export OS_PASSWORD=grou
$ export OS_AUTH_URL=http://k1:5000/v3
$ export OS_USERNAME=grouadmin
$ export OS_PROJECT_DOMAIN_NAME=grou

$ export REQUESTS_LIMIT=10

```


## If you want to use email notifications

```
$ export MAIL_HOST="smtp.gmail.com"
$ export MAIL_PORT=587
$ export MAIL_TLS="true"
$ export MAIL_USER="no-replay+avalidemail@gmail.com"
$ export MAIL_PASS="gmailpass"
```

## Build and run GROU

```
$ nohup make grou run > /tmp/out < /dev/null 2>&1 &
$ while ! curl --connect-timeout 1 http://127.0.0.1:8080 > /dev/null 2> /dev/null; do echo "Waiting 8080/tcp... (press CTRL+C to cancel)"; sleep 5; done
```

## Get Keystone Token

```
$ export TOKEN="$(curl --silent -u grouadmin:grou 127.0.0.1:8080/token/grou | jq -r .token)"
```
or
```
$ export TOKEN="$(openstack token issue -f value -c id)"
```
