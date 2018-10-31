#!/bin/bash
curl localhost:8080/restcomm/ssp/rest?camelop=idp
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=busy
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=idp
sleep 10
curl localhost:8080/restcomm/ssp/rest?camelop=abandon
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=idp
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=routefailure
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=idp
sleep 20
curl localhost:8080/restcomm/ssp/rest?camelop=noanswer
sleep 5
curl localhost:8080/restcomm/ssp/rest?camelop=idp
sleep 10
curl localhost:8080/restcomm/ssp/rest?camelop=answer
sleep 20
curl localhost:8080/restcomm/ssp/rest?camelop=disconnect
sleep 5







