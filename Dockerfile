FROM ubuntu:latest
LABEL authors="shara"

ENTRYPOINT ["top", "-b"]