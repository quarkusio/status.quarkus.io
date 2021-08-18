#! /bin/bash

# login to the OpenShift cluster before launching this script

# delete problematic image
oc delete is ubi-quarkus-native-binary-s2i

# switch to the right project
oc project prod-status-quarkus-io

mvn clean package -Dquarkus.kubernetes.deploy=true -Dquarkus.native.container-build=true -Dnative

# add kubernetes.io/tls-acme: 'true' to the route to renew the SSL certificate automatically

# list the available image streams
# oc get is
# clean up the image streams in case of manifest errors
# oc delete is ubi-quarkus-native-s2i
# oc delete is ubi-quarkus-native-binary-s2i
