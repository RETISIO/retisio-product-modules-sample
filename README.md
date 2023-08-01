# retisio-product-modules-sample
1. extend OOTB aggregate to add additional commands, events, and state
2. override OOTB api with customized implementation
3. add additional APIs along with OOTB apis
4. handle additional events in Read side projections for DB persist, Kafka publish
5. override kafka listeners with customized implementation
6. de-scope OOTB Apis, Aggregates, Projections, Listeners
7. combine multiple OOTB micro-services in extension module and deploy as monolithic application
8. dynamic properties allows to add more custom properties in request, response, command, event, state, message beans
9. dynamic properties can be validated in extension module