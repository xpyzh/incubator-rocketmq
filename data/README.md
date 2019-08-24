### 概述
1. 用于本地环境namesrv和broker的启动，用于debug源码
### namesrv启动配置
* 启动入口: `org.apache.rocketmq.namesrv.NamesrvStartup`
* 设置环境变量: `ROCKETMQ_HOME=/Users/youzhihao/IdeaProjects/open-source/incubator-rocketmq/data`
* 修改日志根目录: `incubator-rocketmq/conf/logback_namesrv.xml`
### broker启动配置
* 启动入口: `org.apache.rocketmq.broker.BrokerStartup`
* 设置环境变量: `ROCKETMQ_HOME=/Users/youzhihao/IdeaProjects/open-source/incubator-rocketmq/data`
* jvm启动指定配置文件: `-c /Users/youzhihao/IdeaProjects/open-source/incubator-rocketmq/data/conf/broker.conf`
* 修改日志根目录: `incubator-rocketmq/conf/logback_broker.xml`
### Producer样例
* 启动入口: `org.apache.rocketmq.example.quickstart.Producer`
### Consumer样例
* 启动入口: `org.apache.rocketmq.example.quickstart.Consumer`