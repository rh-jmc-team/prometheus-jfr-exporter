package com.redhat.jfr.datasource.jfr_prometheus_exporter;

import fi.iki.elonen.NanoHTTPD;
import org.openjdk.jmc.flightrecorder.CouldNotLoadRecordingException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Server extends NanoHTTPD {
    private RecordingService mRecordingService;
    private String mAuthToken;

    public Server(String hostname, int port) {
        super(hostname, port);
    }

    @Override
    public void start() throws IOException {
        setAsyncRunner(new CachedAsyncRunner());

        super.start();
    }

    @Override
    public Response serve(IHTTPSession session) {
        if (!session.getUri().equals("/metrics")) {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", session.getUri() + " is not found on this server.\n");
        }

        if (session.getMethod() != Method.GET) {
            return newFixedLengthResponse(Response.Status.METHOD_NOT_ALLOWED, "text/plain", session.getMethod().name() + " method is not allowed\n");
        }

        if (mRecordingService == null) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", "Recording service not yet registered\n");
        }

        if (mAuthToken != null) {
            String authHeader = session.getHeaders().get("authorization");
            if (authHeader == null || "".equals(authHeader)) {
                Response resp = newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Authentication required\n");
                resp.addHeader("WWW-Authenticate", "Basic realm=\"JFR Prometheus Exporter\"");
                return resp;
            }

            if (!("Basic " + mAuthToken).equals(authHeader)) {
                Response resp = newFixedLengthResponse(Response.Status.UNAUTHORIZED, "text/plain", "Invalid credential\n");
                resp.addHeader("WWW-Authenticate", "Basic realm=\"JFR Prometheus Exporter\"");
                return resp;
            }
        }

        String body;
        try {
            body = mRecordingService.dumpPrometheusMetrics();
        } catch (IOException | CouldNotLoadRecordingException e) {
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, "text/plain", e.getMessage());
        } catch (RecordingService.EmptyRecordingException e) {
            return newFixedLengthResponse(Response.Status.NO_CONTENT, "", "");
        }

        return newFixedLengthResponse(Response.Status.OK, "text/plain", body);
    }

    public void setRecordingService(RecordingService service) {
        mRecordingService = service;
    }

    public RecordingService getRecordingService() {
        return mRecordingService;
    }

    public void setAuthentication(String authentication) {
        mAuthToken = Base64.getEncoder().encodeToString(authentication.getBytes(StandardCharsets.UTF_8));
    }

    class CachedAsyncRunner implements NanoHTTPD.AsyncRunner {
        private ExecutorService executorService = Executors.newCachedThreadPool();
        private final List<ClientHandler> running = Collections.synchronizedList(new ArrayList<>());

        @Override
        public void closeAll() {
            // copy of the list for concurrency
            for (NanoHTTPD.ClientHandler clientHandler : new ArrayList<>(this.running)) {
                clientHandler.close();
            }
        }

        @Override
        public void closed(NanoHTTPD.ClientHandler clientHandler) {
            this.running.remove(clientHandler);
        }

        @Override
        public void exec(NanoHTTPD.ClientHandler clientHandler) {
            executorService.submit(clientHandler);
            this.running.add(clientHandler);
        }
    }
}
