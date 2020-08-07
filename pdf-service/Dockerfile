FROM tomcat:8.5
LABEL maintainer="sysnetcz info@sysnet.cz"
ARG data_dir=/usr/local/data

ADD ./war/pdf-service.war /usr/local/tomcat/webapps/
ENV PDF_DATA_DIR=$data_dir
EXPOSE 8080
VOLUME [$data_dir]

CMD ["catalina.sh", "run"]
