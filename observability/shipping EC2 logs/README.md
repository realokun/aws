## Shipping the application logs to Amazon CloudWatch

1. Install unified CloudWatch agent (a newer replacement to the older CloudWatch Logs agent): https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Agent-on-EC2-Instance.html

    On Amazon Linux2:
    ```
    sudo yum -y install amazon-cloudwatch-agent
    ```

2. Configure CloudWatch agent using Wizard application (need to perform it just once):

    ```
    sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-config-wizard
    ```

    To monitor the application log files, provide the application log file name(s) and their location when prompted by the wizard. It can be changed later directly in the generated JSON file.
    
    The following configuration will be created by the Wizard. Store it in the SSM Parameter Store as a parameter **AmazonCloudWatch-linux**:
    
```json=
{
	"agent": {
		"metrics_collection_interval": 60,
		"run_as_user": "root"
	},
	"logs": {
		"logs_collected": {
			"files": {
				"collect_list": [
					{
						"file_path": "/logs/application.log", 
                        "log_group_name": "/Application/ProductCatalog/appplication",
                        "log_stream_name": "{instance_id}"
					}
				]
			}
		}
	},
	"metrics": {
		"append_dimensions": {
			"AutoScalingGroupName": "${aws:AutoScalingGroupName}",
			"ImageId": "${aws:ImageId}",
			"InstanceId": "${aws:InstanceId}",
			"InstanceType": "${aws:InstanceType}"
		},
		"metrics_collected": {
			"cpu": {
				"measurement": [
					"cpu_usage_idle",
					"cpu_usage_iowait",
					"cpu_usage_user",
					"cpu_usage_system"
				],
				"metrics_collection_interval": 60,
				"totalcpu": false
			},
			"disk": {
				"measurement": [
					"used_percent",
					"inodes_free"
				],
				"metrics_collection_interval": 60,
				"resources": [
					"*"
				]
			},
			"diskio": {
				"measurement": [
					"io_time"
				],
				"metrics_collection_interval": 60,
				"resources": [
					"*"
				]
			},
			"mem": {
				"measurement": [
					"mem_used_percent"
				],
				"metrics_collection_interval": 60
			},
			"swap": {
				"measurement": [
					"swap_used_percent"
				],
				"metrics_collection_interval": 60
			}
		}
	}
}
```

By default, if a line in the log message begins with a non-whitespace character, the CloudWatch closes the previous log message and starts a new log message. 

Therefore, by default, a log statement like this, will result in three separate log entries:
```
LOGGER.error("\nLog correlation Id: {}\n{}", correlationId, ExceptionUtils.getStackTrace(exception));
```

If, instead, it is desirable to output multiple log statements as a single message, insert the following entry after the line 13 in the above configuration: 
```
"multi_line_start_pattern": "{datetime_format}"
```
Thus, the resulting log configuration section will look like this:
```json=
"collect_list": [
    {
	"file_path": "/logs/application.log", 
        "log_group_name": "/Application/ProductCatalog/appplication",
        "log_stream_name": "{instance_id}",
        "multi_line_start_pattern": "{datetime_format}"        
	}
]
```
References:
* Create the CloudWatch agent configuration file: https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/create-cloudwatch-agent-configuration-file.html

3. Launch CloudWatch agent with the reference to the SSM Parameter Store config:
https://docs.aws.amazon.com/AmazonCloudWatch/latest/monitoring/install-CloudWatch-Agent-on-EC2-Instance-fleet.html#start-CloudWatch-Agent-EC2-fleet. 

    ```
    sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:AmazonCloudWatch-linux
    ```
    **Important:** The agent needs to be re-launched every time the parameter value is updated.  

4. Check the status of the CloudWatch agent:

    ```
    sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -m ec2 -a status
    ```
## Installing the AWS CodeDeploy agent on an Amazon Elastic Compute Cloud (Amazon EC2) instance:

1. Bootstrap the EC2 instance: https://aws.amazon.com/premiumsupport/knowledge-center/codedeploy-agent-launch-configuration/

2. Verify the CodeDeploy agent for Amazon Linux is running:

    `sudo service codedeploy-agent status`

## The EC2 User Data section for bootstraping both the Amazon CloudWatch agent and the AWS CodeDeploy agent
```
#!/bin/bash -xe

## CloudWatch Agent Bootstrap ##

sudo yum -y install amazon-cloudwatch-agent

sudo /opt/aws/amazon-cloudwatch-agent/bin/amazon-cloudwatch-agent-ctl -a fetch-config -m ec2 -s -c ssm:AmazonCloudWatch-linux 

## Code Deploy Agent Bootstrap Script ##

exec > >(tee /var/log/user-data.log|logger -t user-data -s 2>/dev/console) 2>&1
AUTOUPDATE=false

function installdep(){

if [ ${PLAT} = "ubuntu" ]; then

  apt-get -y update
  # Satisfying even ubuntu older versions.
  apt-get -y install jq awscli ruby2.0 || apt-get -y install jq awscli ruby



elif [ ${PLAT} = "amz" ]; then
  yum -y update
  yum install -y aws-cli ruby jq

fi

}

function platformize(){

#Linux OS detection#
 if hash lsb_release; then
   echo "Ubuntu server OS detected"
   export PLAT="ubuntu"


elif hash yum; then
  echo "Amazon Linux detected"
  export PLAT="amz"

 else
   echo "Unsupported release"
   exit 1

 fi
}


function execute(){

if [ ${PLAT} = "ubuntu" ]; then

  cd /tmp/
  wget https://aws-codedeploy-${REGION}.s3.amazonaws.com/latest/install
  chmod +x ./install

  if ./install auto; then
    echo "Instalation completed"
      if ! ${AUTOUPDATE}; then
            echo "Disabling Auto Update"
            sed -i '/@reboot/d' /etc/cron.d/codedeploy-agent-update
            chattr +i /etc/cron.d/codedeploy-agent-update
            rm -f /tmp/install
      fi
    exit 0
  else
    echo "Instalation script failed, please investigate"
    rm -f /tmp/install
    exit 1
  fi

elif [ ${PLAT} = "amz" ]; then

  cd /tmp/
  wget https://aws-codedeploy-${REGION}.s3.amazonaws.com/latest/install
  chmod +x ./install

    if ./install auto; then
      echo "Instalation completed"
        if ! ${AUTOUPDATE}; then
            echo "Disabling auto update"
            sed -i '/@reboot/d' /etc/cron.d/codedeploy-agent-update
            chattr +i /etc/cron.d/codedeploy-agent-update
            rm -f /tmp/install
        fi
      exit 0
    else
      echo "Instalation script failed, please investigate"
      rm -f /tmp/install
      exit 1
    fi

else
  echo "Unsupported platform ''${PLAT}''"
fi

}

platformize
installdep
REGION=$(curl -s 169.254.169.254/latest/dynamic/instance-identity/document | jq -r ".region")
execute
```