# System Interfaces Enterprise Integration with Apache Camel

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

## Enterprise Integration with Apache Camel

Apache Camel ([_http://camel.apache.org_](http://camel.apache.org)) is a tool for routing and processing messages with tools for Enterprise Integration Patterns which are described here (and other pages on this site have much other good information about EIP): [_http://www.eaipatterns.com/toc.html_](http://www.eaipatterns.com/toc.html)

Moqui Framework has a Message Endpoint for Camel (_MoquiServiceEndpoint_) that ties it to the Service Facade. This allows services (with **type**=camel) to send the service call as a message to Camel using the _MoquiServiceConsumer_. The endpoint also includes a message producer (_MoquiServiceProducer_) that is available in Camel routing strings as moquiservice.

Here are some example Camel services from the ExampleServices.xml file:

```
`<service verb="localCamelExample" type="camel"
 location="moquiservice:org.moqui.example.ExampleServices.targetCamelExample">
 <in-parameters><parameter name="testInput"/></in-parameters>
 <out-parameters><parameter name="testOutput"/></out-parameters>
</service>
<service verb="targetCamelExample">
 <in-parameters><parameter name="testInput"/></in-parameters>
 <out-parameters><parameter name="testOutput"/></out-parameters>
 <actions>
 <set field="testOutput" value="Here's the input: ${testInput}"/>
 <log level="warn"
 message="targetCamelExample testOutput: ${result.testOutput}"/>
 </actions>
</service>
`
```

When you call the **localCamelExample** service it calls the **targetCamelExample** service through Apache Camel. This is a very simple example of using services with Camel. To get an idea of the many things you can do with Camel the components reference is a good place to start:

[_http://camel.apache.org/components.html_](http://camel.apache.org/components.html)

The general idea is you can:

  
* get message data from a wide variety of sources (file polling, incoming HTTP request, JMS messages, and many more)
  
* transform messages (supported formats include XML, CSV, JSON, EDI, etc)
  
* run custom expressions (even in Groovy!)
  
* split, merge, route, filter, enrich, or apply any of the other EIP tools
  
* send message(s) to endpoint(s)

Camel is a very flexible and feature rich tool so instead of trying to document and demonstrate more here I recommend these books:

  
* 
    

_Instant Apache Camel Message Routing_ by Bilgin Ibryam
    
      
  * [**http://www.packtpub.com/apache-camel-message-routing/book**](http://www.packtpub.com/apache-camel-message-routing/book)
      
  * This book is a quick introduction that will get you going quickly with lots of cool stuff you can do with Camel.
    
  
  
* 
    

_Apache Camel Developer's Cookbook_ by Scott Cranton and Jakub Korab
    
      
  * [**http://www.packtpub.com/apache-camel-developers-cookbook/book**](http://www.packtpub.com/apache-camel-developers-cookbook/book)
      
  * This book has hundreds of tips and examples for using Camel.
    
  
  
* 
    

_Camel in Action_ by Claus Ibsen and Jonathan Anstey
    
      
  * [**http://manning.com/ibsen/**](http://manning.com/ibsen/)
      
  * This is the classic book on Apache Camel. It covers general concepts, various internal details, how to apply the various EIPs, and a summary of many of the components. The web site for this book also has links to a bunch of useful online resources.