package com.example.giftlistb8.services.serviceImpl;

import com.example.giftlistb8.dto.feed.response.FeedResponse;
import com.example.giftlistb8.dto.PaginationResponse;
import com.example.giftlistb8.services.FeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeedServiceImpl implements FeedService {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public PaginationResponse<FeedResponse> getAll(int page, int size) {
        String sql = """
                SELECT u.id as userId,
                       ui.image as image,
                       concat(u.first_name, ' ', u.last_name) as fullName,
                       w.name as wishName,
                       w.image as wishImage,
                       w.status as status,
                       h.date as holidayDate,
                       h.name as holidayName,
                       coalesce((select ui.image
                                 from reserves r
                                          join users u2 on r.user_id = u2.id and w.id = r.wish_id
                                 where is_anonymous is false),
                                null )  as reserveUserPhoto
                FROM users u
                         Join wishes w on u.id = w.user_id
                         Join user_infos ui on u.user_info_id = ui.id
                         JOIN holidays h ON u.id = h.user_id and w.holiday_id = h.id
                         ORDER BY u.id ASC
                """;
        String countSql = "SELECT COUNT(*) FROM (" + sql + ") as count_query";
        int count = jdbcTemplate.queryForObject(countSql, Integer.class);
        int totalCount = (int) Math.ceil((double) count / size);
        int offset = (page - 1) * size;
        sql = String.format(sql + "LIMIT %s OFFSET %s", size, offset);
        List<FeedResponse> feedResponses = jdbcTemplate.query(sql, (resultSet, i) -> new FeedResponse(resultSet.getLong("userId"),resultSet.getString("image"), resultSet.getString("fullName"), resultSet.getString("holidayName"), resultSet.getString("wishName"), resultSet.getString("wishImage"), resultSet.getDate("holidayDate").toLocalDate(), resultSet.getBoolean("status"), resultSet.getString("reserveUserPhoto")

        ));
        return PaginationResponse.<FeedResponse>builder().elements(feedResponses).currentPage(page).pageSize(totalCount).build();
    }

    @Override
    public FeedResponse getById(Long wishId) {
        String sql = """
                SELECT u.id as userId,
                       ui.image as image,
                       concat(u.first_name, ' ', u.last_name) as fullName,
                       w.name as wishName,
                       w.image as wishImage,
                       w.status as status,
                       h.date as holidayDate,
                       h.name as holidayName,
                       coalesce((select ui.image
                                 from reserves r
                                          join users u2 on r.user_id = u2.id and w.id = r.wish_id
                                 where is_anonymous is false),
                                null ) as reserveUserPhoto
                FROM users u
                         Join wishes w on u.id = w.user_id
                         Join user_infos ui on u.user_info_id = ui.id
                         JOIN holidays h ON u.id = h.user_id and w.holiday_id = h.id where w.id=?;
                """;

        Object[] args = {wishId};
        return jdbcTemplate.queryForObject(sql, args, (resultSet, i) -> new FeedResponse(resultSet.getLong("userId"),resultSet.getString("image"), resultSet.getString("fullName"), resultSet.getString("holidayName"), resultSet.getString("wishName"), resultSet.getString("wishImage"), resultSet.getDate("holidayDate").toLocalDate(), resultSet.getBoolean("status"), resultSet.getString("reserveUserPhoto")));
    }
}