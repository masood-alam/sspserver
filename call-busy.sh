#!/bin/bash
curl 127.0.0.1:8080/restcomm/ssp/rest?camelop=idp&caller=1234&called=5678
sleep 5
curl 127.0.0.1:8080/restcomm/ssp/rest?camelop=busy
sleep 1






