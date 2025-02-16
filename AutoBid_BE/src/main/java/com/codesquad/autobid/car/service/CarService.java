package com.codesquad.autobid.car.service;

import com.codesquad.autobid.car.domain.Car;
import com.codesquad.autobid.car.domain.CheckCarResponse;
import com.codesquad.autobid.car.repository.CarRepository;
import com.codesquad.autobid.handler.car.CarHandler;
import com.codesquad.autobid.handler.car.vo.CarVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarHandler carHandler;

    @Transactional
    public List<CheckCarResponse> getCars(Long userId, String accessToken, boolean refresh) {
        // if (accessToken.equals("accessToken")) {
        //     return CarTestUtil.getCarList();
        // }
        List<Car> cars = carRepository.findCarsByUserId(userId);
        if (refresh) {
            List<Car> updatedCars = getUpdatedCars(cars, accessToken, userId);
            List<Car> notExistingCars = retainNotExistingCars(cars, updatedCars);
            carRepository.deleteAll(notExistingCars);
            carRepository.saveAll(updatedCars);
            cars = updatedCars;
        }
        return cars.stream().map(CheckCarResponse::from).collect(Collectors.toList());
    }

    private List<Car> getUpdatedCars(List<Car> cars, String accessToken, Long userId) {
        return carHandler.getCars(accessToken).stream()
                .map(carVO -> carVO.setDistance(carHandler.getDistance(accessToken, carVO.getCarId())))
                .map(carVO -> update(carVO, findByCarId(cars, carVO.getCarId()), userId))
                .collect(Collectors.toList());
    }

    private List<Car> retainNotExistingCars(List<Car> cars, List<Car> updatedCars) {
        return cars.stream()
                .filter((c) -> !updatedCars.stream()
                        .map(uc -> uc.getCarId())
                        .collect(Collectors.toSet())
                        .contains(c.getCarId()))
                .collect(Collectors.toList());
    }

    private Car update(CarVO carVO, Car car, Long userId) {
        if (car != null) {
            car.update(carVO);
            return car;
        }
        return Car.from(carVO, userId);
    }

    private Car findByCarId(List<Car> cars, String carId) {
        return cars.stream()
                .filter(car -> car.getCarId().equals(carId))
                .findAny()
                .orElse(null);
    }
}
