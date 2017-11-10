# Authentication

An OpenStack Keystone access token is needed for the routes that need authentication. In order to generate this token, a Keystone user and a project associated to this user are necessary.


1. Create a project on Openstack Keystone.
2. Associate project to a user or group.
3. The token can be obtained in two ways:

## Get Keystone Token

```
$ export TOKEN="$(curl --silent -u grouadmin:grou 127.0.0.1:8080/token/grou | jq -r .token)"
```

or

```
$ export TOKEN="$(openstack token issue -f value -c id)"

> ${PROJECT} is the previously registered project on OpenStack Keystone.
 ${TOKEN} is the temporary access token provided by OpenStack Keystone to the corresponding ${PROJECT}.


### Alternative way of getting access token (using openstack-client)

In the following example, the access token is obtained through the [openstack-client](https://pypi.python.org/pypi/python-openstackclient).

```
$ TOKEN=$(openstack --os-auth-url http://k1:5000/v3 \
  --os-username ${login} \
  --os-password ${pass} \
  --os-domain-name grou \
  --os-project-name ${PROJECT} \
  token issue -f value -c id)
```
