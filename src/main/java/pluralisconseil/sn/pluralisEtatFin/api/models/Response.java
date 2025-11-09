package pluralisconseil.sn.pluralisEtatFin.api.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.Accessors;

@Getter
 @Setter
 @Accessors(chain = true)
 @NoArgsConstructor
 @JsonInclude(JsonInclude.Include.NON_NULL)
 @JsonIgnoreProperties(ignoreUnknown = true)
 @AllArgsConstructor
 @Builder
 public class Response<T> {

     private Status status;
     private T payload;
     private Object metadata;
     private Object message;

     public static <T> Response<T> badRequest() {
         Response<T> response = new Response<>();
         response.setStatus(Status.BAD_REQUEST);
         return response;
     }
     public static <T> Response<T> invalideRoles() {
         Response<T> response = new Response<>();
         response.setStatus(Status.INVALID_ROLES);
         return response;
     }

     public static <T> Response<T> ok() {
         Response<T> response = new Response<>();
         response.setStatus(Status.OK);
         return response;
     }
     public static <T> Response<T> invalidCredentials() {
         Response<T> response = new Response<>();
         response.setStatus(Status.INVALID_CREDENTIALS);
         return response;
     }

     public static <T> Response<T> created() {
         Response<T> response = new Response<>();
         response.setStatus(Status.CREATED);
         return response;
     }

     public static <T> Response<T> deleted() {
         Response<T> response = new Response<>();
         response.setStatus(Status.DELETED);
         return response;
     }

     public static <T> Response<T> unauthorized() {
         Response<T> response = new Response<>();
         response.setStatus(Status.UNAUTHORIZED);
         return response;
     }

     public static <T> Response<T> validationException() {
         Response<T> response = new Response<>();
         response.setStatus(Status.VALIDATION_EXCEPTION);
         return response;
     }

     public static <T> Response<T> exception() {
         Response<T> response = new Response<>();
         response.setStatus(Status.EXCEPTION);
         return response;
     }

     public static <T> Response<T> notFound() {
         Response<T> response = new Response<>();
         response.setStatus(Status.NOT_FOUND);
         return response;
     }

     public static <T> Response<T> duplicateEmail() {
         Response<T> response = new Response<>();
         response.setStatus(Status.DUPLICATE_EMAIL);
         return response;
     }

     public static <T> Response<T> updatingFailed() {
         Response<T> response = new Response<>();
         response.setStatus(Status.UPDATING_FAILED);
         return response;
     }

     public static <T> Response<T> duplicateReference() {
         Response<T> response = new Response<>();
         response.setStatus(Status.DUPLICATE_REFERENCE);
         return response;
     }

     public static  <T> Response<T> duplicateTelephone() {
         Response<T> response = new Response<>();
         response.setStatus(Status.DUPLICATE_TELEPHONE);
         return response;
     }

     public enum Status {
         OK, BAD_REQUEST, UNAUTHORIZED, VALIDATION_EXCEPTION, EXCEPTION, INVALID_ROLES, INVALID_CREDENTIALS,
         NOT_FOUND, DUPLICATE_EMAIL, DUPLICATE_REFERENCE, DUPLICATE_TELEPHONE, CREATED, DELETED, UPDATING_FAILED
     }

     @Getter
     @Accessors(chain = true)
     @JsonInclude(JsonInclude.Include.NON_NULL)
     @JsonIgnoreProperties(ignoreUnknown = true)
     @Builder
     public static class PageMetadata {
         private final int size;
         private final long totalElements;
         private final int totalPages;
         private final int number;

         public PageMetadata(int size, long totalElements, int totalPages, int number) {
             this.size = size;
             this.totalElements = totalElements;
             this.totalPages = totalPages;
             this.number = number;
         }
     }

 }
