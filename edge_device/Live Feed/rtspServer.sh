#!/bin/bash

rpicam-vid -t 0 -n --codec libav --libav-format mpegts -o - | cvlc stream:///dev/stdin --sout '#rtp{sdp=rtsp://:8554/doorbell}' --rtsp-timeout=0