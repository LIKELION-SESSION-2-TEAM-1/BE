package g3pjt.service.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoomRequest {
    private String name;
    private LocalDate startDate;
    private LocalDate endDate;
    private String travelStyle;
}
