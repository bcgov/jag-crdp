## Templates to create openshift components related to jag-crdp-process-transformer deployment

### Command to execute template
1) Login to OC using login command
2) Run below command in each env. namespace dev/test/prod
   ``oc process -f jag-crdp-process-transformer.yaml --param-file=jag-crdp-process-transformer.env | oc apply -f -``


