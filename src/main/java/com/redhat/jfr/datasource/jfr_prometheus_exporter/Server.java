package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import fi.iki.elonen.NanoHTTPD;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;

import java.io.IOException;

// A ultra-light simple non-blocking, IO-multiplexed TCP server implementation
class Server extends NanoHTTPD {
    private RecordingService mRecordingService;

    public Server(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!session.getUri().equals("/metrics")) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", session.getUri() + " is not found on this server.");
        }

        if (session.getMethod() != Method.GET) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", session.getMethod().name() + " method is not allowed");
        }

        if (mRecordingService == null) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Recording service not yet registered");
        }

        String resp;
        try {
            resp = mRecordingService.dumpPrometheusMetrics();
        } catch (IOException | CouldNotLoadRecordingException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        } catch (RecordingService.EmptyRecordingException e) {
            return newFixedLengthResponse(Response.Status.NO_CONTENT, "", "");
        }

        return newFixedLengthResponse(Response.Status.OK, "text/plain", resp);
    }

    public void setRecordingService(RecordingService service) {
        mRecordingService = service;
    }

    public RecordingService getmRecordingService() {
        return mRecordingService;
    }
}
