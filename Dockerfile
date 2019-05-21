FROM java:8
RUN /bin/cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && echo 'Asia/Shanghai' >/etc/timezone

WORKDIR /home/apps/
ADD target/manage-gateway.jar .
ADD start.sh .

ENTRYPOINT ["/bin/bash", "/home/apps/start.sh"]