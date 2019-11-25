package io.slingr.api.common;

import com.idea2.utils.Json;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPart;

import javax.ws.rs.client.WebTarget;
import java.io.InputStream;

/**
 * Simple Rest client over a unique URI
 *
 * Created by lefunes on 21/03/16.
 */
public class SimpleRestClient extends RestClient {
    private static final Logger logger = Logger.getLogger(SimpleRestClient.class);

    public SimpleRestClient(String apiUri) throws RestException {
        super(apiUri, isSecureConnection(apiUri));
    }

    public SimpleRestClient setHeader(String name, Object value) {
        setupDefaultHeader(name, value);
        return this;
    }

    public SimpleRestClient setParam(String name, String value) {
        setupDefaultParam(name, value);
        return this;
    }

    public SimpleRestClient path(String path) {
        super.setPath(path);
        return this;
    }

    public SimpleRestClient retries(Integer retries) {
        this.setRetries(retries);
        return this;
    }

    public SimpleRestClient connectionTimeout(Integer timeout) {
        this.setConnectionTimeout(timeout);
        return this;
    }

    public SimpleRestClient readTimeout(Integer timeout) {
        this.setReadTimeout(timeout);
        return this;
    }

    public SimpleRestClient silenceLogger() {
        this.setSilenceLogger(true);
        return this;
    }

    public SimpleRestClient disableConvertContentToString() {
        this.setConvertContentToString(false);
        return this;
    }

    public static SimpleRestClient uri(String apiUri){
        return new SimpleRestClient(apiUri);
    }

    public Json get() throws RestException {
        return super.get(null);
    }

    public Json get(String path) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.get(target);
    }

    public Json post() throws RestException {
        return super.post(null);
    }

    public Json post(String path) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.post(target);
    }

    public Json post(Json content) throws RestException {
        return super.post(null, content);
    }

    public Json post(String path, Json content) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.post(target, content);
    }

    public Json post(MultiPart content) throws RestException {
        return super.post(null, content);
    }

    public Json post(String path, MultiPart content) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.post(target, content);
    }

    public Json put() throws RestException {
        return super.put(null);
    }

    public Json put(String path) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.put(target);
    }

    public Json put(Json content) throws RestException {
        return super.put(null, content);
    }

    public Json put(String path, Json content) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.put(target, content);
    }

    public Json patch() throws RestException {
        return super.patch(null);
    }

    public Json patch(Json content) throws RestException {
        return super.patch(null, content);
    }

    public Json head() throws RestException {
        return super.head(null);
    }

    public Json delete() throws RestException {
        return super.delete(null);
    }

    public Json delete(String path) throws RestException {
        WebTarget target = getApiTarget().path(path);
        return super.delete(target);
    }

    public Json options() throws RestException {
        return super.options(null);
    }

    public Json execute(RestMethod method, Json jsonRequest){
        switch (method){
            case POST: return post(jsonRequest);
            case PUT: return put(jsonRequest);
            case PATCH: return patch(jsonRequest);
            case DELETE: return delete();
            case HEAD: return head();
            case OPTIONS: return options();
        }
        return get();
    }

    public InputStream download(){
        return super.download(null);
    }

    public InputStream download(String path) {
        WebTarget target = getApiTarget().path(path);
        return super.download(target);
    }

    public InputStream download(boolean throwException){
        return super.download(null, throwException);
    }

    public InputStream download(String path, boolean throwException) {
        WebTarget target = getApiTarget().path(path);
        return super.download(target, throwException);
    }
}
