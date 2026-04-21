# Logic and Services Overview of XML Actions

##### Wiki Spaces
    
        
            [__ Mantle Business Artifacts](https://moqui.org/m/docs/mantle)    
    
    
            [__ Moqui Applications](https://moqui.org/m/docs/apps)    
    
    
            [__ Moqui Community](https://moqui.org/m/docs/moqui)    
    
    
            [__ Moqui Framework](https://moqui.org/m/docs/framework)    
    

    
    
    

##### Page Tree
            [Moqui Framework](https://moqui.org/m/docs/framework)
    
    

    

 
            [All Pages](https://moqui.org/m/alldocs/framework)
    
        
    
    

# Overview of XML Actions

The _xml-actions-${version}.xsd_ file has thorough annotations for detailed documentation, this section is just an overview of what is available to help you get started. You can view the annotations through most good XML editors (including the better Java IDEs or IDE plugins), in the XSD file itself, or in the PDF on [moqui.org](https://moqui.org)that is generated from the XSD file.

Here is a summary of the most important XML Actions elements to be aware of:

  
      
  
  
    setSet a **field**, either **from** another field or from a **value**, optionally specifying the **type**, a **default-value**, and whether to **set-if-empty**.
    ifConditionally run the elements directly under the if element, or in the if.then element. The condition can be in the if.**condition** attribute or in compare and expression elements under the if.condition element (combined with and or or element, negated by the not element). For alternate actions use the _else-if_and _else_ subelements.
    whileRepeat the subelements as long as the condition is true. Just like the if element the condition can be in the if.**condition** attribute or in the _if.condition_ element.
    iterateIterate over elements in the given **list**, creating a field in the context using the name in the **entry** attribute. If the field named in the **list** attribute is a Map, iterates over the map _entries_ and the _key_ for each entry is put in the context using the name in the **key** attribute. Also creates context fields ${**entry**}_index and ${**entry**}_has_next.
    scriptRun any kind of script the Resource Facade can run at the specified **location** or the Groovy script in the text under this element (inline script).
    service-callCall the service specified in the **name** attribute, using the inputs in the **in-map** attribute (which is a Groovy expression, so can use the square-brace [] syntax for an inline Map) or field-map subelements and putting the outputs in the **out-map**. Can optionally be **async** and **include-user-login**. If the service results in an error the simple method will return immediately unless **ignore-error** equals true.
    entity-find-oneFind a single record for **entity-name** and put it in an _EntityValue_ object in **value-field** using attributes including **auto-field-map, cache**, and **for-update**, and subelements including _field-map_ and _select-field_.
    entity-findFind records for **entity-name** and put an _EntityList_ object in **list** using attributes including **cache**, **for-update**, **distinct**, **offset**, and **limit**, and subelements including _search-form-inputs_, _date-filter_, _econdition_, _econditions_, _econdition-object_, _having-econditions_, _select-field_, _order-by_, _limit-range_, _limit-view_, and _use-iterator_.
    entity-find-countFind the count of the number of records that match the given conditions. Conditions and other application options follow the same structure as the entity-find operation.
    entity-make-valueCreate a **value-field** entity value object for the given **entity-name** and optionally set fields based on a **map**.
    entity-createCreate (**or-update**) a record for the **value-field** entity value.
    entity-updateUpdate the record for the **value-field** entity value.
    entity-deleteDelete the record corresponding to the **value-field** entity value.
    entity-setSet fields to **include** (pk, nonpk, or all) on _EntityValue_ object in **value-field** from **map** (defaults to context) with an optional **prefix** and **set-if-empty**.
    entity-sequenced-id-primaryFor **value-field** of an entity with a single primary key field, populate that primary key field with a sequenced value (the sequence name is the full entity name).
    entity-sequenced-id-secondaryFor **value-field** of an entity with a two field primary key and one field already populated, populate the other with a secondary sequenced key with the value of the highest existing secondary field for records matching the populated field, plus 1.
    entity-dataFor the given **mode**, load or asset the Entity Facade XML at the specified **location**.
    filter-map-listFilter the **list** and put the results in **to-list** if specified or back in **list** if not. Use one or more _field-map_ or _date-filter_ subelements to specify how to filter the list.
    order-map-listOrder (sort) a **list** of Map objects by the fields specified in order-by subelements.
    messageAdd the text under the message element to the Message Facade to the errors list if **error**=true or the message list otherwise.
    check-errorsChecks the Message Facade error message list (ec.message.errors) and if not empty returns with an error, otherwise does nothing.
    returnReturns immediately. Can optionally specify a **message** to add to the Message Facade errors list if **error**=true or the message list otherwise.
    logLog the **message** at the specified **level**.