package com.aws.vokunev.prodcatalog.dao;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.aws.vokunev.prodcatalog.model.InstanceMetadata;
import com.aws.vokunev.prodcatalog.util.CorrelatingLogger;

/**
 * This class implements a DAO patetrn for accessing the EC2 instance metadata.
 */
@Component
public class InstanceMetadataAccessor extends ApiAccessor {

    @Autowired
    private CorrelatingLogger logger;

    @PostConstruct
    private void init() {
        logger.init(InstanceMetadataAccessor.class);
    }

    /**
     * This method fetches the instance metadata in case if the application
     * has been deployed to an EC2 instance.
     * 
     * @return an instance of a {@link InstanceMetadata} or null if not available.
     */
    public InstanceMetadata getInstanceMetadata() throws IOException {

        InstanceMetadata result = null;

        // Fetch the instance id
        String instance_id = invokeGetAPIRequest("http://169.254.169.254/latest/meta-data/instance-id");
        if (instance_id != null) {
            if (result == null) {
                result = new InstanceMetadata();
            }
            result.setInstanceId(instance_id);
        }

        // Fetch the instance AZ
        String instance_az = invokeGetAPIRequest("http://169.254.169.254/latest/meta-data/placement/availability-zone");
        if (instance_az != null) {
            if (result == null) {
                result = new InstanceMetadata();
            }
            result.setAvailabilityZone(instance_az);
        }

        logger.info(String.format("Instance metadata received: %s", result));

        return result;
    }
}