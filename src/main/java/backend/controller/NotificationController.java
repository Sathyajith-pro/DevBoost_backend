package backend.controller;

import backend.model.NotificationModel;
import backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = {"http://localhost:3000"}, maxAge = 3600)
@Validated
public class NotificationController {

    @Autowired
    private NotificationRepository notificationRepository;

    @GetMapping("/{userId}")
    public ResponseEntity<Page<NotificationModel>> getNotifications(
            @PathVariable @NotBlank @Size(min = 1, max = 50) String userId,
            @RequestParam(defaultValue = "0") @Positive int page,
            @RequestParam(defaultValue = "10") @Positive int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {
        
        try {
            String[] sortParams = sort.split(",");
            Sort.Direction direction = Sort.Direction.fromString(sortParams[1]);
            Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortParams[0]));
            
            Page<NotificationModel> notifications = notificationRepository.findByUserId(userId, pageable);
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/{userId}/unread")
    public ResponseEntity<List<NotificationModel>> getUnreadNotifications(
            @PathVariable @NotBlank @Size(min = 1, max = 50) String userId) {
        try {
            List<NotificationModel> unreadNotifications = notificationRepository
                    .findByUserIdAndIsReadFalse(userId);
            return ResponseEntity.ok(unreadNotifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping
    public ResponseEntity<NotificationModel> createNotification(
            @RequestBody @Validated NotificationModel notification) {
        try {
            notification.setCreatedAt(LocalDateTime.now());
            notification.setRead(false);
            NotificationModel savedNotification = notificationRepository.save(notification);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedNotification);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    @PutMapping("/{id}/markAsRead")
    public ResponseEntity<?> markAsRead(@PathVariable @NotBlank String id) {
        try {
            Optional<NotificationModel> notificationOpt = notificationRepository.findById(id);
            if (notificationOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Notification not found");
            }
            
            NotificationModel notification = notificationOpt.get();
            if (notification.isRead()) {
                return ResponseEntity.ok("Notification already marked as read");
            }
            
            notification.setRead(true);
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            return ResponseEntity.ok("Notification marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking notification as read");
        }
    }

    @PutMapping("/markAllAsRead/{userId}")
    public ResponseEntity<?> markAllAsRead(
            @PathVariable @NotBlank @Size(min = 1, max = 50) String userId) {
        try {
            List<NotificationModel> unreadNotifications = notificationRepository
                    .findByUserIdAndIsReadFalse(userId);
            
            unreadNotifications.forEach(notification -> {
                notification.setRead(true);
                notification.setReadAt(LocalDateTime.now());
            });
            
            notificationRepository.saveAll(unreadNotifications);
            return ResponseEntity.ok("All notifications marked as read");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error marking notifications as read");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable @NotBlank String id) {
        try {
            if (!notificationRepository.existsById(id)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Notification not found");
            }
            
            notificationRepository.deleteById(id);
            return ResponseEntity.ok("Notification deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting notification");
        }
    }

    @DeleteMapping("/user/{userId}")
    public ResponseEntity<?> deleteAllUserNotifications(
            @PathVariable @NotBlank @Size(min = 1, max = 50) String userId) {
        try {
            notificationRepository.deleteByUserId(userId);
            return ResponseEntity.ok("All user notifications deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting user notifications");
        }
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleException(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("An error occurred: " + e.getMessage());
    }
}