package udemy.adrian_wiech.happy_hotel_app.booking;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BookingServiceTest {
	@InjectMocks
	private BookingService bookingService;

	@Mock
	private PaymentService paymentServiceMock;

	@Mock
	private RoomService roomServiceMock;

	// @Mock
	// private BookingDAO bookingDAOMock;

	@Spy
	private BookingDAO bookingDAOSpy;

	@Mock
	private MailSender mailSenderMock;

	@Captor
	private ArgumentCaptor<Double> doubleArgsCaptor;

	@BeforeAll
	static void printDefaultValuesReturnedFromMockedMethods() {
		// all the mocked methods return default values as empty lists, null objects & false primitives
		System.out.println(
			"mock(RoomService.class).getAvailableRooms() -> " +
			mock(RoomService.class).getAvailableRooms() +
			"\nmock(RoomService.class).findAvailableRoomId() -> " +
			mock(RoomService.class).findAvailableRoomId(null) +
			"\nmock(RoomService.class).getRoomCount() -> " +
			mock(RoomService.class).getRoomCount()
		);
	}

	/*
	@BeforeEach
	void setup() {
		// create mocked & spied data for BookingService's 4 dependencies
		// mock: dummy objects with no real logic | spy: real objects with real logic
		this.paymentServiceMock = mock(PaymentService.class);
		this.roomServiceMock = mock(RoomService.class);
		this.mailSenderMock = mock(MailSender.class);
		// this.bookingDAOMock = mock(BookingDAO.class);
		this.bookingDAOSpy = spy(BookingDAO.class);

		this.bookingService = new BookingService(
			paymentServiceMock,
			roomServiceMock,
			bookingDAOSpy,
			mailSenderMock
		);

		this.doubleArgsCaptor = ArgumentCaptor.forClass(Double.class);
	}
	*/

	@Nested
	@DisplayName("getGuests() tests")
	class GetGuestsTests {
		@Test
		void should_count_all_guests() {
			// given
			int expectedValue = 0;

			// when
			int actualValue = bookingService.getGuests();

			// then
			assertEquals(expectedValue, actualValue);
		}

		@Test
		void should_return_all_guests_when_1_room_available() {
			// given
			int expectedNumberOfGuests = 2;

			List<Room> availableRoom = Collections.singletonList(new Room("room 1", expectedNumberOfGuests));

			// make a mocked method return custom values, instead of its default ones
			// when(roomServiceMock.getAvailableRooms()).thenReturn(availableRoom);
			given(roomServiceMock.getAvailableRooms()).willReturn(availableRoom);

			// when
			int actualNumberOfGuests = bookingService.getGuests();

			// then
			assertEquals(expectedNumberOfGuests, actualNumberOfGuests);
		}

		@Test
		void should_return_all_guests_when_multiple_rooms_available() {
			// given
			Room room1 = new Room("room 1", 2);
			Room room2 = new Room("room 2", 5);

			List<Room> rooms = Arrays.asList(room1, room2);

			when(roomServiceMock.getAvailableRooms()).thenReturn(rooms);

			int expectedNumberOfGuests = room1.getCapacity() + room2.getCapacity();

			// when
			int actualNumberOfGuests = bookingService.getGuests();

			// then
			assertEquals(expectedNumberOfGuests, actualNumberOfGuests);
		}

		@Test
		void should_return_different_availableRooms_when_calling_thenReturn_multiple_times() {
			// given
			String roomId = "room 1";
			int expectedNumberOfGuests1stThenReturn = 2;
			int expectedNumberOfGuests2ndThenReturn = 0;

			when(roomServiceMock.getAvailableRooms())
					.thenReturn(
						Collections.singletonList(new Room(roomId, expectedNumberOfGuests1stThenReturn))
					)
					.thenReturn(Collections.emptyList());

			// when
			int actualNumberOfGuests1stThenReturn = bookingService.getGuests();
			int actualNumberOfGuests2ndThenReturn = bookingService.getGuests();

			// then
			assertAll(
				() -> assertEquals(expectedNumberOfGuests1stThenReturn, actualNumberOfGuests1stThenReturn),
				() -> assertEquals(expectedNumberOfGuests2ndThenReturn, actualNumberOfGuests2ndThenReturn)
			);
		}
	}

	@Nested
	@DisplayName("calculatePriceInUSD() tests")
	class CalculatePriceInUSDTests {
		@Test
		void should_calculatePrice_correctly_with_correct_input() {
			// given
			String guestId = "1";
			int bookedYear = 2022;
			int bookedMonth = 1;
			int firstBookedDay = 1;
			int lastBookedDay = 5;
			int numberOfGuests = 2;
			double price = 50.0;
			boolean isBookingPrepaid = false;

			BookingRequest bookingRequest = new BookingRequest(
			guestId,
			LocalDate.of(bookedYear, bookedMonth, firstBookedDay),
			LocalDate.of(bookedYear, bookedMonth, lastBookedDay),
			numberOfGuests,
			isBookingPrepaid
			);

			double expectedValue = (lastBookedDay - firstBookedDay) * numberOfGuests * price;

			// when
			double actualValue = bookingService.calculatePriceInUSD(bookingRequest);

			// then
			assertEquals(expectedValue, actualValue);
		}
	}

	@Nested
	@DisplayName("calculatePriceInEuro() tests")
	class CalculatePriceInEuroTests {
		@Test
		void should_calculate_correct_price_in_euro() {
			try(
				MockedStatic<CurrencyConverter> mockedStaticCurrencyConverter = mockStatic(CurrencyConverter.class)
			) {
				// given
				BookingRequest bookingRequest = new BookingRequest(
					"2",
					LocalDate.of(2022, 1, 1),
					LocalDate.of(2022, 1, 5),
					2,
					false
				);

				// double expectedBookingPrice = 400.0;
				double expectedBookingPrice = 400.0 * 0.8;

				/*
				mockedStaticCurrencyConverter.when(
					() -> CurrencyConverter.toEuro(anyDouble())
				).thenReturn(expectedBookingPrice);
				*/

				// explained in sec 3 | vid 21: using Mockito Answers
				mockedStaticCurrencyConverter.when(
				() -> CurrencyConverter.toEuro(anyDouble())
				).thenAnswer(invocationOnMock -> (double) invocationOnMock.getArgument(0) * 0.8);

				// when
				double actualBookingPrice = bookingService.calculatePriceInEuro(bookingRequest);

				// then
				assertEquals(expectedBookingPrice, actualBookingPrice);
			}
		}
	}

	@Nested
	@DisplayName("verifyBooking() tests")
	class VerifyBookingTests {
		@Test
		void should_throw_BusinessException_when_roomId_not_found() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"3",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				false
			);

			when(roomServiceMock.findAvailableRoomId(bookingRequest)).thenThrow(BusinessException.class);

			// when
			Executable executable = () -> bookingService.verifyBooking(bookingRequest);

			// then
			assertThrows(BusinessException.class, executable);
		}

		@Test
		void should_throw_UnsupportedOperationException_with_any_bookingRequest() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"4",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			when(
				paymentServiceMock.pay(
					any(BookingRequest.class),
					anyDouble()
				)
			).thenThrow(UnsupportedOperationException.class);

			// when
			Executable executable = () -> bookingService.verifyBooking(bookingRequest);

			// then
			assertThrows(UnsupportedOperationException.class, executable);
		}

		@Test
		void should_throw_UnsupportedOperationException_when_price_too_expensive() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"5",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			when(
				paymentServiceMock.pay(
					any(BookingRequest.class),
					eq(400.0)
			)
			).thenThrow(UnsupportedOperationException.class);

			// when
			Executable executable = () -> bookingService.verifyBooking(bookingRequest);

			// then
			assertThrows(UnsupportedOperationException.class, executable);
		}

		@Test
		void should_pay_when_prepaid_is_true() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"6",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			// explained in sec 3 | vid 19: strict stubbing
			// lenient().when(paymentServiceMock.pay(any(), anyDouble())).thenReturn("1");

			// when
			bookingService.verifyBooking(bookingRequest);

			// then
			// verify(paymentServiceMock, times(1)).pay(bookingRequest, 400.0);
			then(paymentServiceMock).should(times(1)).pay(bookingRequest, 400.0);

			verifyNoMoreInteractions(paymentServiceMock);
		}

		@Test
		void should_not_pay_when_prepaid_is_false() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"7",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				false
			);

			// when
			bookingService.verifyBooking(bookingRequest);

			// then
			verify(paymentServiceMock, never()).pay(any(), anyDouble());
		}

		@Test
		void should_verifyBooking_when_input_is_ok() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"8",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			// when
			String bookingId = bookingService.verifyBooking(bookingRequest);

			// then
			// verify(bookingDAOMock).save(bookingRequest);
			verify(bookingDAOSpy).save(bookingRequest);

			System.out.println("bookingId -> " + bookingId);
		}

		@Test
		void should_throw_UnsupportedOperationException_when_sendBookingConfirmation_returns_any_value() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"9",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			// to throw an exception from a void method (sendBookingConfirmation)
			doThrow(new UnsupportedOperationException()).when(mailSenderMock).sendBookingConfirmation(any());

			// when
			Executable executable = () -> bookingService.verifyBooking(bookingRequest);

			// then
			assertThrows(UnsupportedOperationException.class, executable);
		}

		@Test
		void should_not_throw_UnsupportedOperationException_when_sendBookingConfirmation_returns_nothing() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"10",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			// to make sure that a void method returns nothing (sendBookingConfirmation)
			doNothing().when(mailSenderMock).sendBookingConfirmation(any());

			// when
			bookingService.verifyBooking(bookingRequest);

			// then: no exception thrown
		}

		@Test
		void should_pay_correct_prices_when_booking_is_verified_once() {
			// given
			double expectedArg = 400.0;

			BookingRequest bookingRequest = new BookingRequest(
				"11",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			// when
			bookingService.verifyBooking(bookingRequest);

			// then
			verify(paymentServiceMock, times(1)).pay(eq(bookingRequest), doubleArgsCaptor.capture());

			double capturedArg = doubleArgsCaptor.getValue();

			System.out.println("capturedArg -> " + capturedArg);

			assertEquals(expectedArg, capturedArg);
		}

		@Test
		void should_pay_correct_prices_when_booking_is_verified_more_than_once() {
			// given
			double pricePerNight = 100.0;

			BookingRequest bookingRequest1 = new BookingRequest(
				"12",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			BookingRequest bookingRequest2 = new BookingRequest(
				"13",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 2),
				2,
				true
			);

			List<Double> expectedPrices = Arrays.asList(pricePerNight * 4, pricePerNight);

			// when
			bookingService.verifyBooking(bookingRequest1);
			bookingService.verifyBooking(bookingRequest2);

			// then
			verify(paymentServiceMock, times(2)).pay(any(), doubleArgsCaptor.capture());

			List<Double> capturedArgs = doubleArgsCaptor.getAllValues();

			System.out.println("capturedArgs -> " + capturedArgs);

			assertEquals(expectedPrices, capturedArgs);
		}
	}

	@Nested
	@DisplayName("cancelBooking() tests")
	class CancelBookingTests {
		@Test
		void should_cancelBooking_when_input_is_ok() {
			// given
			BookingRequest bookingRequest = new BookingRequest(
				"14",
				LocalDate.of(2022, 1, 1),
				LocalDate.of(2022, 1, 5),
				2,
				true
			);

			bookingRequest.setRoomId("1408");

			String bookingId = "1";

			doReturn(bookingRequest).when(bookingDAOSpy).get(bookingId);

			// when
			bookingService.cancelBooking(bookingId);
		}
	}
}
