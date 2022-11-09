## Templates to create openshift components related to jag-crdp-transmit-receiver deployment

### Command to execute template
1) Login to OC using login command
2) Run below command in each env. namespace dev/test/prod
   ``oc process -f jag-crdp-transmit-receiver.yaml --param-file=jag-crdp-transmit-receiver.env | oc apply -f -``


