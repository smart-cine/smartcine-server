package org.example.cinemamanagement.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import org.example.cinemamanagement.common.Status;
import org.example.cinemamanagement.common.VnPayConstant;
import org.example.cinemamanagement.dto.PaymentDTO;
import org.example.cinemamanagement.model.Account;
import org.example.cinemamanagement.model.Payment;
import org.example.cinemamanagement.model.PickSeat;
import org.example.cinemamanagement.payload.request.AddPaymentRequest;
import org.example.cinemamanagement.payload.request.OrderRequestDTO;
import org.example.cinemamanagement.repository.*;
import org.example.cinemamanagement.service.PaymentService;
import org.example.cinemamanagement.utils.VnPayHelper;
import org.modelmapper.ModelMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.*;

@Service
public class PaymentServiceImpl implements PaymentService {
    private PaymentRepository paymentRepository;
    private UserRepository userRepository;
    private CinemaRepository cinemaRepository;
    private PickSeatRepository pickSeatRepository;
    private ModelMapper modelMapper;

    private PerformRepository performRepository;

    private CinemaLayoutSeatRepository cinemaLayoutSeatRepository;


    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              UserRepository userRepository,
                              CinemaRepository cinemaRepository,
                              PickSeatRepository pickSeatRepository,
                              ModelMapper modelMapper,
                              PerformRepository performRepository,
                              CinemaLayoutSeatRepository cinemaLayoutSeatRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
        this.cinemaRepository = cinemaRepository;
        this.pickSeatRepository = pickSeatRepository;
        this.modelMapper = modelMapper;
        this.performRepository = performRepository;
        this.cinemaLayoutSeatRepository = cinemaLayoutSeatRepository;
    }

    @Override
    public PaymentDTO getPayment(UUID id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
        PaymentDTO paymentDTO = new PaymentDTO();
        modelMapper.map(paymentDTO, Payment.class);
        return paymentDTO;
    }

    @Override
    public String addPayment(AddPaymentRequest req) {
        Account accountTemp = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Account account = userRepository.findById(accountTemp.getId()).orElseThrow(
                () -> new RuntimeException("User not found")
        );


        if (req.getAmount() == null)
            throw new RuntimeException("Error payment (amount is NULL)");

        Payment payment = new Payment();
        payment.setAccount(account);
//        payment.setCinema(cinema);
//        payment.setAmount(req.getAmount());
        UUID paymentId = paymentRepository.save(payment).getId();
        System.out.print(paymentId);

        return "Successfully";
    }

    @Override
    @Transactional
    public Map<String, Object> createOrder(HttpServletRequest request, OrderRequestDTO orderRequestDTO) throws UnsupportedEncodingException {

        try {
            Account account = (Account) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

            if (!RedisServiceImpl.exists("pickseat:" + orderRequestDTO.getPerformId() + ":" + account.getId())) {
                throw new RuntimeException("You have not picked any seat yet");
            }

            Payment paymentTemp = paymentRepository.findTheLastPaymentByAccountIdAndPerformId(
                    account.getId(),
                    orderRequestDTO.getPerformId()
            );

            if (paymentTemp != null && paymentTemp.getStatus() == Status.PENDING) {
                throw new RuntimeException("You have a pending payment");
            }


            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");

            String createDate = VnPayHelper.generateDate(false);
            String expireDate = VnPayHelper.generateDate(true);

            Map<String, Object> payload = new HashMap<>() {
                {
                    put("vnp_Version", VnPayConstant.VNP_VERSION);
                    put("vnp_Command", VnPayConstant.VNP_COMMAND_ORDER);
                    put("vnp_TmnCode", VnPayConstant.VNP_TMN_CODE);
                    put("vnp_Amount", String.valueOf(orderRequestDTO.getAmount() * 100));
                    put("vnp_CurrCode", VnPayConstant.VNP_CURRENCY_CODE);
                    put("vnp_TxnRef", VnPayHelper.getRandomNumber(8));
                    put("vnp_OrderInfo", orderRequestDTO.getOrderInfo());
                    put("vnp_OrderType", VnPayConstant.ORDER_TYPE);
                    put("vnp_Locale", VnPayConstant.VNP_LOCALE);
                    put("vnp_ReturnUrl", VnPayConstant.VNP_RETURN_URL);
                    put("vnp_IpAddr", VnPayHelper.getIpAddress(request));
                    put("vnp_CreateDate", createDate);
                    put("vnp_ExpireDate", expireDate);
                }
            };

            String queryUrl = getQueryUrl(payload).get("queryUrl")
                    + "&vnp_SecureHash="
                    + VnPayHelper.hmacSHA512(VnPayConstant.SECRET_KEY, getQueryUrl(payload).get("hashData"));

            String paymentUrl = VnPayConstant.VNP_PAY_URL + "?" + queryUrl;
            payload.put("redirect_url", paymentUrl);


            Payment payment = Payment.builder()
                    .account(userRepository.findById(account.getId()).orElseThrow(() -> new RuntimeException("User not found")))
                    // convert createDate variable to timestamp type

                    .dateCreate(Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                            formatter.parse(createDate))
                    ))
                    .dateExpire(
                            Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                                    formatter.parse(expireDate))
                            )
                    )
                    .perform(performRepository.findById(orderRequestDTO.getPerformId()).orElseThrow(() -> new RuntimeException("Perform not found")))
                    .status(Status.PENDING)
                    .vnpTxnRef(payload.get("vnp_TxnRef").toString())
                    .item(null)
                    .build();

            paymentRepository.save(payment);

            System.out.println("pickseat:" + orderRequestDTO.getPerformId() + ":" + account.getId());
            RedisServiceImpl.expire("pickseat:" + orderRequestDTO.getPerformId() + ":" + account.getId(),
                    Duration.ofMinutes(12).getSeconds()
            );
            return payload;
        } catch (Exception ex) {
            throw new RuntimeException("Error: " + ex.getMessage());
        }
    }

    private Map<String, String> getQueryUrl(Map<String, Object> payload) throws UnsupportedEncodingException {
        List<String> fieldNames = new ArrayList(payload.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator itr = fieldNames.iterator();
        while (itr.hasNext()) {

            String fieldName = (String) itr.next();
            String fieldValue = (String) payload.get(fieldName);
            if ((fieldValue != null) && (fieldValue.length() > 0)) {

                // Build hash data
                hashData.append(fieldName);
                hashData.append('=');
                hashData.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));

                // Build query
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString()));
                query.append('=');
                query.append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString()));
                if (itr.hasNext()) {

                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        return new HashMap<>() {
            {
                put("queryUrl", query.toString());
                put("hashData", hashData.toString());
            }
        };
    }

    @Transactional
    public void handlePayment(Map<String, String> params) {
        try {
            String vnpResponseCode = params.get("vnp_ResponseCode");
            String vnpTxnRef = params.get("vnp_TxnRef");
            String vnpAmount = params.get("vnp_Amount");
            String vnpOrderInfo = params.get("vnp_OrderInfo");
            String vnpTransactionNo = params.get("vnp_TransactionNo");
            String vnpTransactionStatus = params.get("vnp_TransactionStatus");
            String vnpPayDate = params.get("vnp_PayDate");

            SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
            Payment payment = paymentRepository.findByVnpTxnRefAndDateCreate
                    (vnpTxnRef,
                            Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                    .format(new SimpleDateFormat("yyyyMMddHHmmss").parse(vnpPayDate)))
                    );

            switch (vnpResponseCode) {
                case "00":
                    payment.setStatus(Status.SUCCESS);
                    Set<String> seatIds = RedisServiceImpl.smembers("pickseat:" + payment.getPerform().getId() + ":" + payment.getAccount().getId());
                    for (String seatId : seatIds) {
                        pickSeatRepository.save(PickSeat.builder()
                                .account(payment.getAccount())
                                .perform(payment.getPerform())
                                .layoutSeat(cinemaLayoutSeatRepository.findById(UUID.fromString(seatId)).orElseThrow(() -> new RuntimeException("Seat not found")))
                                .createAt(Timestamp.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                                        formatter.parse(vnpPayDate))))
                                .build());
                    }
                    RedisServiceImpl.del("pickseat:" + payment.getPerform().getId() + ":" + payment.getAccount().getId());
                    break;
                case "01":
                    payment.setStatus(Status.PENDING);
                    break;
                default:
                    payment.setStatus(Status.FAILED);
                    RedisServiceImpl.del("pickseat:" + payment.getPerform().getId() + ":" + payment.getAccount().getId());
                    break;
            }

            paymentRepository.save(payment);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
