#!/bin/bash
curl 172.17.0.2:8080/restcomm/ssp/rest?camelop=idp
sleep 5
curl 172.17.0.2:8080/restcomm/ssp/rest?camelop=answer
sleep 45
curl 172.17.0.2:8080/restcomm/ssp/rest?camelop=disconnect







