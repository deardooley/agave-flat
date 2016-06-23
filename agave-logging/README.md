# Agave Logging Service
*****

This service tracks usage information on the individual API endpoints. Unlike the BAM logging, this is integrated into the Agave code base and catches all invocations made on the physical services. This should be run as a public, non-advertised service with a bypass in the apache config.
