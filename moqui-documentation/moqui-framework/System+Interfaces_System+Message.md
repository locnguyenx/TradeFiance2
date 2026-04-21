# System Interfaces System Message

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

# System Message

## Introduction

The System Message functionality in Moqui Framework handles message queuing and processing for both incoming and outgoing messages.

  
* message queue for store and send for outgoing messages, and store and process for incoming messages
  
* failure and retry handling with automatic retry via configurable service jobs
  
* full history of incoming and outgoing messages for auditing, debugging, reprocessing, etc
  
* UI in the System app to find, view, update, and manually retry sending and processing of messages
  
* configuration on _SystemMessageType_ for the services to use for producing, sending, receiving, and processing messages
  
* configuration on _SystemMessageRemote_ for remote connection parameters (usage varies by transport method)
  
* generic endpoint to receive incoming messages over HTTP
  
* generic JSON over HTTP send and receive services
  
* SFTP outgoing message drop and incoming message pick up support in the optional **moqui-sftp** component

## Recommended Practices

### Incoming Messages

  
* always store original text from external system
  
* avoid interim data structures, for reusable code used for multiple incoming file formats use a service with nested parameters and transform code to nested Map+List structures for the service call (not to persist or use otherwise)
  
* large messages with multiple entries (transactions, etc):
    
      
  * if can use database data to determine that an entry has already been processed may process entire message at once using check each entry for retries until all are done
      
  * if no way to query DB to see if done then process large message by splitting into one _SystemMessage_ record per entry so success/failure/retry is tracked for each
    
  

### Outgoing Messages

  
* for each integration define:
    
      
  * what needs to be sent (for example which orders)
      
  * which SystemMessageType and SystemMessageRemote to use (this may be configurable)
    
  
  
* check for existing _SystemMessage_ record by the primary ID (such as orderId), _SystemMessageRemote_ and/or _SystemMessageType_
    
      
  * in other words rely on data for one and only one message, don't rely on code (ECA rules, service calls, etc)
    
  
  
* once message text generated queue for sending using **org.moqui.impl.SystemMessageServices.queue#SystemMessage** service
    
      
  * by default this service will try to send the message immediately using the configured send service, set **sendNow** to false to not send right away
      
  * make sure the _SystemMessage_ record has been created and the transaction committed before calling **queue#SystemMessage** because it uses separate transactions to manage _SystemMessage_ state independent of the send service