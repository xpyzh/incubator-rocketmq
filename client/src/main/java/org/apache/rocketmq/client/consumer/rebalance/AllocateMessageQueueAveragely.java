/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.rocketmq.client.consumer.rebalance;

import com.alibaba.fastjson.JSONObject;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.rocketmq.client.consumer.AllocateMessageQueueStrategy;
import org.apache.rocketmq.client.log.ClientLogger;
import org.apache.rocketmq.common.message.MessageQueue;
import org.apache.rocketmq.logging.InternalLogger;

/**
 * Average Hashing queue algorithm
 * DefaultMQPushConsumer的默认负载均衡算法
 */
public class AllocateMessageQueueAveragely implements AllocateMessageQueueStrategy {
    private final InternalLogger log = ClientLogger.getLog();

    /**
     * cidAll的数量不能超过mq队列的数量，否则多出来的cid分配不到messageQueue
     * 详细看下面的单元测试
     * 结论，一个messageQueue只能被一个cid消费，这个回头看broker内部实现
     * @param currentCID：MQClientInstance的cid
     * @param mqAll:所有队列
     * @param cidAll:所有MQClientInstance实例
     * @author youzhihao
     */
    @Override
    public List<MessageQueue> allocate(String consumerGroup, String currentCID, List<MessageQueue> mqAll,
        List<String> cidAll) {
        if (currentCID == null || currentCID.length() < 1) {
            throw new IllegalArgumentException("currentCID is empty");
        }
        if (mqAll == null || mqAll.isEmpty()) {
            throw new IllegalArgumentException("mqAll is null or mqAll empty");
        }
        if (cidAll == null || cidAll.isEmpty()) {
            throw new IllegalArgumentException("cidAll is null or cidAll empty");
        }

        List<MessageQueue> result = new ArrayList<MessageQueue>();
        if (!cidAll.contains(currentCID)) {
            log.info("[BUG] ConsumerGroup: {} The consumerId: {} not in cidAll: {}",
                consumerGroup,
                currentCID,
                cidAll);
            return result;
        }

        int index = cidAll.indexOf(currentCID);
        int mod = mqAll.size() % cidAll.size();
        int averageSize =
            mqAll.size() <= cidAll.size() ? 1 : (mod > 0 && index < mod ? mqAll.size() / cidAll.size()
                + 1 : mqAll.size() / cidAll.size());
        int startIndex = (mod > 0 && index < mod) ? index * averageSize : index * averageSize + mod;
        int range = Math.min(averageSize, mqAll.size() - startIndex);
        for (int i = 0; i < range; i++) {
            result.add(mqAll.get((startIndex + i) % mqAll.size()));
        }
        return result;
    }

    @Override
    public String getName() {
        return "AVG";
    }

    // 负载均衡算法进行测试
    // 所有策略都是相同结果:cidAll的数量不能超过mq队列的数量，否则多出来的cid分配不到messageQueue
    // 当部分queue失效(master和所有slave都失效)，可以看出,只有AllocateMessageQueueConsistentHash不会改变当前消费的负载均衡
    public static void main(String[] args) {
        int mqCount = 12;
        int cidCount = 6;
        Set<Integer> failQueueIds = new HashSet<Integer>();
        //failQueueIds.add(4);
        //failQueueIds.add(5);
        String groupName = "test";
        List<AllocateMessageQueueStrategy> strategyList = new ArrayList<AllocateMessageQueueStrategy>();
        strategyList.add(new AllocateMessageQueueAveragely());
        strategyList.add(new AllocateMessageQueueAveragelyByCircle());
        strategyList.add(new AllocateMessageQueueConsistentHash());
        List<String> cidAll = new ArrayList<String>();
        List<MessageQueue> mqAll = new ArrayList<MessageQueue>();
        for (int i = 0; i < cidCount; i++) {
            cidAll.add("cid-" + i);
        }
        for (int i = 0; i < mqCount; i++) {
            //模拟fail的节点
            if (failQueueIds.contains(i)) {
                continue;
            }
            mqAll.add(new MessageQueue(groupName, "brokerName", i));
        }
        //计算每一个cid，分配到的messageQueue
        for (AllocateMessageQueueStrategy strategy : strategyList) {
            System.out.println(MessageFormat.format("-----------开始计算负载均衡:{0}----------", strategy.getClass()));
            System.out.println(MessageFormat.format("cidCount={0},mqCount={1}", mqCount, cidCount));
            for (String cid : cidAll) {
                List<MessageQueue> result = strategy.allocate(groupName, cid, mqAll, cidAll);
                System.out.println(MessageFormat.format("cidName={0},messageQueue={1}", cid, JSONObject.toJSON(result)));
            }
        }
    }
}
