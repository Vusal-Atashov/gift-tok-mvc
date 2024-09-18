package service;

import domain.entity.Users;
import java.util.List;

public interface WinnerSelectionService {
   void calculateWinningChances(List<Users> users);
   Users selectWinner();
}
