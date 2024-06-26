package com.aws.vokunev.cdk;

import software.amazon.awscdk.core.Construct;
import software.amazon.awscdk.core.Duration;
import software.amazon.awscdk.core.Stack;
import software.amazon.awscdk.services.sns.Topic;
import software.amazon.awscdk.services.sns.subscriptions.SqsSubscription;
import software.amazon.awscdk.services.sqs.Queue;

/**
 * The object of this type represents am AWS CloudFormation "template", written
 * in Java language.
 */
public class HelloCdkStack extends Stack {

    /**
     * The maximum number of Amazon SQS queues that can be created with this template. 
     */
    public final static int MAX_NUMBER_OF_QUEUES = 10; 

    /**
     * This method creates an AWS CloudFormation stack with a provided name.
     * The stack contains an Amazon SNS topic "MyCdkTopic". It also creates
     * a specified number of Amazon SQS queues with the specified name prefix.
     * Each of these queues is subscribed to the SNS topic.
     * 
     * @param parent        reference to the parent construct
     * @param stackName     name of the AWS CloudFormation stack to be created
     * @param totalQueues   number of Amazon SQS queues to be created 
     * @param namePrefix    prefix for the Amazon SQS queue name
     * @throws RuntimeException if the numberOfQueues parameter exceeds the value of MAX_NUMBER_OF_QUEUES
     */
    public HelloCdkStack(final Construct parent, final String stackName, final int totalQueues,
            final String namePrefix) {
        super(parent, stackName, null);

        // Provide parameter valudation logic
        if(totalQueues > MAX_NUMBER_OF_QUEUES) {
            throw new RuntimeException(String.format("The requested number of queues (%s) exceeds the maximum allowed amount of %s.", totalQueues, MAX_NUMBER_OF_QUEUES));
        }

        // Create an SNS topic
        final Topic topic = Topic.Builder.create(this, "MyTopicID")
            .topicName("MyDemoCDKTopic")
            .displayName("An SNS topic created with CDK")
            .build();

        // Create a specified number of SQS queues and subscribe each to the SNS topic
        for (int i = 0; i < totalQueues; i++) {

            Queue queue = Queue.Builder.create(this, "MyQueueID" + i)
                .queueName(namePrefix + i)
                .visibilityTimeout(Duration.seconds(300))
                .retentionPeriod(Duration.days(5))
                .build();

            topic.addSubscription(new SqsSubscription(queue));
        }
    }
}
