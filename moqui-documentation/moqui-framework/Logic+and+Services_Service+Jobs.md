# Logic and Services Service Jobs

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

# Service Jobs

Moqui provide support to configure ad-hoc (explicitly executed) or schedule jobs using `moqui.service.job.ServiceJob` and `moqui.service.job.ServiceJobParameter` entities.
**ServiceCallJob** interface is used for ad-hoc (explicit) run of configured service jobs. User can track execution of Jobs using `moqui.service.job.ServiceJobRun` records.

Some important fields of _moqui.service.job_.**ServiceJob** entity that you should know-

  
* _jobName_: used to store unique job name.
  
* _serviceName_: used to store Service name (like ${path}.${verb}${noun}) that you want to call on service job run.
  
* _topic_: used to store a value of notification that will be sent to the current user and all users configured using `moqui.service.job.ServiceJobUser` records
  
* _cronExpression_:  used to configure instances of CronTrigger, a subclass of `org.quartz.Trigger`. A cron expression is a string consisting of six or seven subexpressions (fields) that describe individual       details of the schedule.
    For more details on cron expression, see the documentation at
    
      
  * [http://cron-parser.com](http://cron-parser.com)
  * [http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/tutorial-lesson-06.html](http://www.quartz-scheduler.org/documentation/quartz-2.2.x/tutorials/tutorial-lesson-06.html)

_moqui.service.job_.**ServiceJobParameter** entity stores parameter name, value pair that passes to service on Service Job run.

Methods of _ServiceCallJob_ interface

  
* _parameter_(String name, Object value): Single name, value pairs to put in the  parameters passed to the service.
  
* _parameters_(Map<String, ?> context): Map of name, value pairs that make up the context (in parameters) passed to the service.
  
* _run_():  Run a service job.
    
      
  * Service jobs will always run asynchronously.
      
  * If the _ServiceJob.topic_ field has a value of notification will be sent to the current user and all users configured using `moqui.service.job.ServiceJobUser` records. The _NotificationMessage_.**message**           field will be the results of this service call.
      
  * It return the jobRunId for the corresponding `moqui.service.job.ServiceJobRun` record.
    
  

For Example-

```
`  ec.service.job("ImportEntityDataSnapshot").parameters(context).run()
`
```

_ImportEntityDataSnapshot_ job is used to import the Entity Data snapshots. Here **job**(String jobName) method is used to get  a service caller to call a service job. There must be a `moqui.service.job.ServiceJob` record for this jobName.

The list of Service Jobs available at  System => Server Admin => Service Jobs screen.

Some examples of a schedule job from _MoquiInstallData.xml_ file, which is in place by default in Moqui-

  
* _clean_ArtifactData_daily_ job clean the Artifact Data: ArtifactHit, ArtifactHitBin every night at 2:00 am

```
`   <moqui.service.job.ServiceJob jobName="clean_ArtifactData_daily" description="Clean Artifact Data: ArtifactHit, ArtifactHitBin"  serviceName="org.moqui.impl.ServerServices.clean#ArtifactData" cronExpression="0 0 2 * * ?" paused="N">
       <parameters parameterName="daysToKeep" parameterValue="90"/>
   </moqui.service.job.ServiceJob>
`
```

  
* _clean_ServiceJobRun_daily_ job clean ServiceJobRun Data every night at 2:00 am

```
`<moqui.service.job.ServiceJob jobName="clean_ServiceJobRun_daily" description="Clean ServiceJobRun Data"  serviceName="org.moqui.impl.ServiceServices.clean#ServiceJobRun" 
cronExpression="0 0 2 * * ?" paused="N">
        <parameters parameterName="daysToKeep" parameterValue="30"/>
</moqui.service.job.ServiceJob>
`
```

  
* _send_AllProducedSystemMessages_frequent_ job send all produced system messages in every 15 minutes

```
`<moqui.service.job.ServiceJob jobName="send_AllProducedSystemMessages_frequent" description="Send All Produced SystemMessages"           serviceName="org.moqui.impl.SystemMessageServices.send#AllProducedSystemMessages" cronExpression="0 0/15 * * * ?" paused="N"/>
`
```