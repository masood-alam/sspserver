#!/bin/bash
./docker-call-busy.sh
sleep 5
./docker-call-abandon.sh
sleep 5
./docker-call-routefailure.sh
sleep 5
./docker-call-noanswer.sh
sleep 5
./docker-call-answer.sh







