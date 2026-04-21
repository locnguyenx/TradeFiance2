# User Interface Sending and Receiving Email

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

# Sending and Receiving Email

The first step to sending and receiving email is to setup an EmailServer with something like this record loaded:

```
`<moqui.basic.email.EmailServer emailServerId="SYSTEM"
   smtpHost="mail.test.com" smtpPort="25" smtpStartTls="N" smtpSsl="N"
   storeHost="mail.test.com" storePort="143" storeProtocol="imap"
   storeDelete="N" mailUsername="TestUser" mailPassword="TestPassword"/>
`
```

Note that these are all example values and should be changed to real values, especially for the **smtpHost**, **storeHost**, **mailUsername** and **mailPassword** fields. The **store*** fields are for the remote mail store for incoming email. Here are some other common values for the port fields:

  
* **smtpPort**: 25 (SMTP), 465 (SSMTP), 587 (SSMTP)
  
* **storePort** for **storeProtocol**=imap: 143 (IMAP), 585 (IMAP4-SSL), 993 (IMAPS)
  
* **storePort** for **storeProtocol**=pop3: 110 (POP3), 995 (SSL-POP)

If you need to work with multiple email servers, just add _EmailServer_ records with the settings for each. When sending an email using an email template the _EmailServer_ to use is specified on the _EmailTemplate_ record with the **emailServerId** field.

Speaking of EmailTemplate, the next step for sending an email is to create one. Here is an example from HiveMind PM for sending a task update notification email:

```
`<moqui.basic.email.EmailTemplate emailTemplateId="HM_TASK_UPDATE"
   description="HiveMind Task Update Notification"
   emailServerId="SYSTEM" webappName="webroot"
   bodyScreenLocation="component://HiveMind/screen/TaskUpdateNotification.xml"
   fromAddress="[[email protected]](/cdn-cgi/l/email-protection)" ccAddresses="" bccAddresses=""
   subject="Task Updated: ${document._id} - ${document.WorkEffort.name}"/>`
```

The general idea is to define a screen that will be rendered for the body when the email is sent (**bodyScreenLocation**). The email body screen is a little bit different from normal UI screens because there is no Web Facade available when it is rendered as it is not part of a web request. The URL prefixes (domain name, port, etc) are generated based on webapp settings in the Moqui Conf XML file, which is why it is necessary to specify a **webappName** which is matched against the _moqui-conf.webapp-list.webapp_.**name** attribute.

The **subject** is also a simple template of sorts, it is a Groovy String that is expanded when the email is sent using the same context as rendering the body. The **fromAddress** field is required, and you can optionally specify **ccAddresses** and **bccAddresses**.

Attachments to an _EmailTemplate_ can be added with the _EmailTemplateAttachment_ entity. The filename to use on the email must be specified using the **fileName** field. The attachment itself comes from rendering a screen specified with the **attachmentLocation** field. The **screenRenderMode** field is passed to the _ScreenRender_ to specify the type of output to get from the screen. It is also used to determine the MIME/content type. If empty the content at **attachmentLocation** will be sent over without screen rendering and its MIME type will be based on its extension. This can be used to generate XSL:FO that is transformed to a PDF and attached to the email with by setting **screenRenderMode** to _xsl-fo_.

Once the _EmailServer_ and _EmailTemplate_ are defined you can send email using the _org.moqui.impl.EmailServices_.**send#EmailTemplate** service. When calling this service pass in the **emailTemplateId** parameter to identify the EmailTemplate. As mentioned above the _EmailServer_ will be determined based on the _EmailTemplate_.**emailServerId** field.

The email addresses to send the message to are passed in the **toAddresses** parameter which is a plain _String_ and can have multiple comma-separated addresses. The parameters used to render the email screen are separate from the context of the service and are passed to it in the **bodyParameters** input parameter. By default the **send#EmailTemplate** service saves details about the outgoing message in a record of the _EmailMessage_ entity. To disable this pass in false in the **createEmailMessage** parameter. The output parameters are **messageId** which is the value put in the Message-ID email header field, and **emailMessageId** if a EmailMessage record is created.

The _EmailMessage_ entity is used for both outgoing and incoming email messages. For outgoing messages sent using the **send#EmailTemplate** service the status (**statusId**) starts out as Sent (actually sets it to Ready, sends the email, then sets it to Sent) and may be changed to Viewed if there is open message tracking based on an image request (usually with the **emailMessageId** as a parameter or path element). If the message is returned undeliverable the status may be changed to Bounced.

An EmailMessage may also be sent manually instead of from a template and in that case the status would start out as Draft. Once the user is done with the message they would change the status to Ready, and then when it is actually sent the status would change to Sent. Incoming messages start in the Received status and can be changed to the Viewed status after they are initially opened.

For email threads the _EmailMessage_ entity has **rootEmailMessageId** for the original messages that all messages in the thread are grouped under, and **parentEmailMessageId** for the message the current message was an immediate reply to.

Receiving email follows a very different path. The _org.moqui.impl.EmailServices_.**poll#EmailServer** service polls a IMAP or POP3 mailbox based on the settings on the EmailServer entity. It takes a single input parameter, the **emailServerId**. Generally this will be run as a scheduled service.

For each message found in the mailbox and not yet marked as seen this service calls the Email ECA (EMECA) rules for it. These are similar to the Entity and Service ECA rules but there is no special trigger, just the receiving of an email. The conditions can be used to only run the actions for a particular to address or tag in the subject like or any other criteria desired.

The context for the condition and actions will include a **headers** _Map_ with all of the email headers in it (either _String_, or _List_ of _String_ if there are more than one of the header), and a **fields** Map with the following: **toList**, **ccList**, **bccList**, **from**, **subject**, **sentDate**, **receivedDate**, **bodyPartList**. The ***List** fields are _List_ of _String_, and the ***Date** fields are _java.util.Date_ objects. For a service that is called directly with this context setup you can implement the _org.moqui.EmailServices_.**process#EmailEca** interface.

The actions and services they call can do anything with the incoming email. To save the incoming message you can use the _org.moqui.impl.EmailServices_.**save#EcaEmailMessage** service.