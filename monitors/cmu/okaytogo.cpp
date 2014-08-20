void okayToGo(
float car_x, float car_y, float obs_x, float obs_y,
float v, float w, float dt, float max_accv,
float inner_radius, float outer_radius, 
bool& okayToGoBool1, bool& okayToGoBool2)
{
  
  double f_v = fabs(v);//f_V = maxObsVel;
  float accv = max_accv;
  float timePeriod = dt;
  float X[5];
  
  X[0] = fabs((float)v);
  X[1] = (float)car_x; X[2] = (float)car_y;
  X[3] = (float)obs_x; X[4] = (float)obs_y;
  double d0 = (double)((accv / accv + 1) *
  (accv / 2 * timePeriod * timePeriod + f_v * timePeriod));
  double D[3];
  D[0] = d0 + outer_radius;
  D[1] = (double)((f_v / accv) + timePeriod * (accv / accv + 1));
  D[2] = (double)(1 / (2 * accv));

  int result = dwmonitor(X, D);
  
//  if (result == 1)
//  {
//    okayToGob = true;
//  }
  
  if (result != 1)
  {  
//you are going to be unsafe soon
    okayToGoBool1 = false; 
    D[0] = d0 + inner_radius;
    int result2 = dwmonitor(X, D);
//    printf("\n Conservative Not Passed");
    if (result2 == 1)
    {
//      printf("\n-------- back up you are too close to the obstacle -----");
      okayToGoBool2 = true;
    }
    else{okayToGoBool2 =false;}
  }
  else{okayToGoBool1 = true;okayToGoBool2 =true;}
  
return;
}
