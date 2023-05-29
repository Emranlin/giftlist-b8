package com.example.giftlistb8.repositories.custom.impl;

import com.example.giftlistb8.dto.charity.response.CharityResponseProfile;
import com.example.giftlistb8.dto.charity.response.CharityResponseWIthComplaint;
import com.example.giftlistb8.dto.complaint.response.ComplaintResponse;
import com.example.giftlistb8.dto.user.response.WhoComplaintResponse;
import com.example.giftlistb8.dto.wish.response.WishResponseProfile;
import com.example.giftlistb8.dto.wish.response.WishResponseWithComplaint;
import com.example.giftlistb8.exceptions.NotFoundException;
import com.example.giftlistb8.repositories.custom.ComplaintRepositoryCustom;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ComplaintRepositoryCustomImpl implements ComplaintRepositoryCustom {
    private final JdbcTemplate jdbcTemplate;

    @Override
    public ComplaintResponse getAllComplaints() {

        String sql = """
                SELECT CONCAT(u.first_name, ' ', u.last_name) AS full_name,
                       ui.image AS user_image,
                       ch.name AS charity_name,
                       ch.date_of_issue AS charity_date_of_issue,
                       ch.image AS charity_image,
                       c.complaint AS complaint_text,
                       (SELECT ui2.image
                            FROM user_infos ui2
                                JOIN users u2 on ui2.id = u2.user_info_id
                            WHERE u2.id = c.user_id
                       ) AS complaint_user_image
                            FROM charities ch
                       JOIN users u ON ch.user_id = u.id
                       JOIN user_infos ui ON u.user_info_id = ui.id
                       JOIN charities_complaints cc on ch.id = cc.charity_id
                       JOIN complaints c on cc.complaints_id = c.id;
                 """;

        String sql2 = """
                SELECT CONCAT(u.first_name, ' ', u.last_name) AS full_name,
                       ui.image AS user_image,
                       wh.name AS wish_name,
                       wh.date_of_holiday AS wish_holiday_date,
                       wh.image AS wish_image,
                       c.complaint AS complaint_text,
                       (SELECT ui2.image
                        FROM user_infos ui2
                                 JOIN users u2 on ui2.id = u2.user_info_id
                        WHERE u2.id = c.user_id
                       ) AS complaint_user_image
                FROM wishes wh
                         JOIN users u ON wh.user_id = u.id
                         JOIN user_infos ui ON u.user_info_id = ui.id
                         JOIN wishes_complaints cc on wh.id = cc.wish_id
                         JOIN complaints c on cc.complaints_id = c.id;
                 """;


        ComplaintResponse complaintResponse = new ComplaintResponse();

        List<CharityResponseWIthComplaint> charityResponses = jdbcTemplate.query(sql, (resultSet, i) ->
                new CharityResponseWIthComplaint(
                        resultSet.getString("full_name"),
                        resultSet.getString("user_image"),
                        resultSet.getString("charity_name"),
                        resultSet.getDate("charity_date_of_issue"),
                        resultSet.getString("charity_image"),
                        resultSet.getString("complaint_user_image"),
                        resultSet.getString("complaint_text")
                ));

        complaintResponse.setCharityResponseWIthComplaints(charityResponses);
        List<WishResponseWithComplaint> wishResponses = jdbcTemplate.query(sql2, (resultSet, i) ->
                new WishResponseWithComplaint(
                        resultSet.getString("full_name"),
                        resultSet.getString("user_image"),
                        resultSet.getString("wish_name"),
                        resultSet.getDate("wish_holiday_date"),
                        resultSet.getString("wish_image"),
                        resultSet.getString("complaint_user_image"),
                        resultSet.getString("complaint_text")
                ));
        complaintResponse.setWishResponseWithComplaints(wishResponses);

        return complaintResponse;
    }

    @Override
    public WishResponseProfile wishGetById(Long id) {
        String sql = """
                SELECT u.id,
                concat(u.first_name,' ',u.last_name) AS fullName,
                       ui.image AS userImage,
                       ui.phone_number,
                       w.name AS wishName,
                       w.description,
                       h.date,
                       h.name AS holidayName,
                       w.status,
                       w.image AS wishImage,
                       case when r.id is null then false else true end as hasReserve,
                       r.is_anonymous,
                       ui2.image
                FROM wishes w
                    JOIN users u on w.user_id = u.id
                    JOIN user_infos ui on u.user_info_id = ui.id
                    JOIN holidays h on w.holiday_id = h.id
                    LEFT JOIN reserves r on w.id = r.wish_id
                    LEFT JOIN users u2 on r.user_id = u2.id
                    LEFT JOIN user_infos ui2 on u2.user_info_id = ui2.id
                WHERE w.id = ?
                """;

        String sql2 = """
                SELECT u.id AS userId,
                        concat(u.first_name,' ',u.last_name) as fullName,
                        ui.image,
                        c.complaint
                        FROM wishes w
                            JOIN wishes_complaints wc on wc.wish_id = w.id
                            JOIN complaints c on wc.complaints_id = c.id
                            JOIN users u on c.user_id = u.id
                            JOIN user_infos ui on u.user_info_id = ui.id
                        WHERE w.id = ?;
                """;

        WishResponseProfile wishResponseProfile;
        List<WhoComplaintResponse> complaints;

        try {
            wishResponseProfile = jdbcTemplate.queryForObject(sql, new Object[]{id},
                    (rs, rowNum) -> {
                        WishResponseProfile response = new WishResponseProfile();
                        response.setUserId(rs.getLong("id"));
                        response.setFullName(rs.getString("fullName"));
                        response.setUserImage(rs.getString("userImage"));
                        response.setPhoneNumber(rs.getString("phone_number"));
                        response.setWishName(rs.getString("wishName"));
                        response.setDescription(rs.getString("description"));
                        response.setDate(rs.getDate("date").toLocalDate());
                        response.setHolidayName(rs.getString("holidayName"));
                        response.setStatus(rs.getString("status"));
                        response.setWishImage(rs.getString("wishImage"));
                        response.setReserved(rs.getBoolean("hasReserve"));
                        response.setAnonymous(rs.getBoolean("is_anonymous"));
                        response.setReserveUserImage(rs.getString("image"));
                        return response;
                    });
        } catch (NotFoundException ex) {
            return null; // handle case where no row is returned from the query
        }

        complaints = jdbcTemplate.query(sql2, new Object[]{id},
                (rs, rowNum) -> {
                    WhoComplaintResponse response = new WhoComplaintResponse();
                    response.setUserId(rs.getLong("userId"));
                    response.setFullName(rs.getString("fullName"));
                    response.setUserImage(rs.getString("image"));
                    response.setCausesOfComplaint(rs.getString("complaint"));
                    return response;
                });

        List<WhoComplaintResponse> existWhoComplaintResponse = wishResponseProfile.getWhoComplaintResponses();
        if (existWhoComplaintResponse == null) {
            existWhoComplaintResponse = new ArrayList<>();
            wishResponseProfile.setWhoComplaintResponses(existWhoComplaintResponse);
        }

        wishResponseProfile.getWhoComplaintResponses().addAll(complaints);
        return wishResponseProfile;
    }

    @Override
    public CharityResponseProfile charityGetById(Long id) {
        String sql = """
                SELECT u.id AS userId,
                       concat(u.first_name,' ',u.last_name) AS fullName,
                       ui.image AS userImage,
                       ui.phone_number,
                       ch.name AS charityName,
                       ch.description,
                       ch.category,
                       ch.sub_category,
                       ch.state,
                       ch.date_of_issue,
                       ch.image AS charityImage,
                       case when r.id is null then false else true end as hasReserve,
                       r.is_anonymous
                FROM charities ch
                         JOIN users u on ch.user_id = u.id
                         JOIN user_infos ui on u.user_info_id = ui.id
                         JOIN reserves r on ch.id = r.charity_id
                WHERE ch.id = ?;
                """;

        String sql2 = """
                SELECT u.id AS userId,
                       concat(u.first_name,' ',u.last_name) as fullName,
                       ui.image,
                       c.complaint
                FROM charities ch
                         JOIN charities_complaints cc on cc.charity_id = ch.id
                         JOIN complaints c on cc.complaints_id = c.id
                         JOIN users u on c.user_id = u.id
                         JOIN user_infos ui on u.user_info_id = ui.id
                WHERE ch.id = ?;
                """;

        CharityResponseProfile charityResponseProfile;
        List<WhoComplaintResponse> complaintResponses;

        charityResponseProfile = jdbcTemplate.queryForObject(sql, new Object[]{id},
                (rs, rowNum) -> {
                    CharityResponseProfile response = new CharityResponseProfile();
                    response.setUserId(rs.getLong("userId"));
                    response.setFullName(rs.getString("fullName"));
                    response.setUserImage(rs.getString("userImage"));
                    response.setPhoneNumber(rs.getString("phone_number"));
                    response.setCharityName(rs.getString("charityName"));
                    response.setDescription(rs.getString("description"));
                    response.setCategory(rs.getString("category"));
                    response.setSubCategory(rs.getString("sub_category"));
                    response.setState(rs.getString("state"));
                    response.setDateAdded(rs.getDate("date_of_issue").toLocalDate());
                    response.setCharityImage(rs.getString("charityImage"));
                    response.setReserved(rs.getBoolean("hasReserve"));
                    response.setAnonymous(rs.getBoolean("is_anonymous"));
                    return response;
                });


        complaintResponses = jdbcTemplate.query(sql2, new Object[]{id},
                (rs, rowNum) -> {
                    WhoComplaintResponse response = new WhoComplaintResponse();
                    response.setUserId(rs.getLong("userId"));
                    response.setFullName(rs.getString("fullName"));
                    response.setUserImage(rs.getString("image"));
                    response.setCausesOfComplaint(rs.getString("complaint"));
                    return response;
                });

        List<WhoComplaintResponse> whoComplaintResponses = charityResponseProfile.getWhoComplaintResponses();
        if (whoComplaintResponses == null) {
            whoComplaintResponses = new ArrayList<>();
            charityResponseProfile.setWhoComplaintResponses(whoComplaintResponses);
        }
        charityResponseProfile.getWhoComplaintResponses().addAll(complaintResponses);
        return charityResponseProfile;
    }
}