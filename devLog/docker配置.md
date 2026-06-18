# docker配置

failed to connect to the docker API at unix:///var/run/docker.sock; check if the path is correct and if the daemon is running: dial unix /var/run/docker.sock: connect: no such file or directory

确认docker服务存在：

```bash
$ systemctl status docker
```

报错变为：

```text
permission denied while trying to connect to the docker API at unix:///var/run/docker.sock
```

权限问题。  
进入该目录，查看权限：

```bash
ls -l
```

输出如下：

```text
drwx------  5 root   root    120  6月18日 20:50 docker
-rw-r--r--  1 root   root      5  6月18日 20:50 docker.pid
srw-rw----  1 root   docker    0  6月18日 20:50 docker.sock
```

说明属主：root，组：docker。  
只有root或docker组成员可访问。

将当前用户加入docker组：  

```bash
sudo usermod -aG docker $USER
```

重新登录当前用户，验证是否生效：

```bash
groups
```

若命令`docker ps`执行成功，则说明配置成功。

新的报错：

```text
org.springframework.boot.docker.compose.core.DockerProcessStartException: Unable to start 'docker-compose' process or use 'docker compose'. Is docker correctly installed?
```

检查docker compose：

```bash
docker compose version
docker-compose version
```

二者不能正常输出。说明compose未安装。

安装docker compose v2插件：

```bash
sudo zypper install docker-compose
```

安装后再次验证docker compose：

```bash
phrolova@phrolova:~> docker-compose version
 docker-compose: 命令未找到
phrolova@phrolova:~> docker compose version
Docker Compose version 5.1.4
```

这是正常现象。docker-compose是旧版命令，不必需。

新的报错：

```text
org.springframework.boot.docker.compose.core.ProcessExitException: 'docker compose --file /home/phrolova/CodeProject/Vitaelens/vitaelens-backend/compose.yaml --ansi never up --no-color --detach --wait' failed with exit code 1.

Stdout:


Stderr:
 Image mysql:latest Pulling 
 Image mysql:latest Error failed to resolve reference "docker.io/library/mysql:latest": failed to do request: Head "https://registry-1.docker.io/v2/library/mysql/manifests/latest": dial tcp 162.125.18.133:443: i/o timeout
Error response from daemon: failed to resolve reference "docker.io/library/mysql:latest": failed to do request: Head "https://registry-1.docker.io/v2/library/mysql/manifests/latest": dial tcp 162.125.18.133:443: i/o timeout
```

docker拉取mysql镜像，访问docker hub时网络超时。

给 Docker daemon 配置DNS：

```bash
cd /etc/docker
sudo nano daemon.json
```

在其中插入：

```json
{
  "dns": ["8.8.8.8", "1.1.1.1"]
}
```

重启：

```bash
sudo systemctl restart docker
```

该方法无效。  
尝试新的方法。

docker不会自动使用系统代理。需手动设置。

查找docker.service服务配置文件：

```bash
sudo systemctl status docker
```

输出中的loaded行显示docker.service服务配置文件路径。

编辑文件，在其中添加代理环境变量(端口替换为代理端口)：

```bash
[Service]
Environment="HTTP_PROXY=http://127.0.0.1:8966"
Environment="HTTPS_PROXY=http://127.0.0.1:8966"
```

重载service配置，重启docker服务：

```bash
sudo systemctl daemon-reload
sudo systemctl restart docker
```
