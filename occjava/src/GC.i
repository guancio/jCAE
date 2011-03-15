%{
#include <GC_MakeSegment.hxx>
%}

%typemap(javacode) GC_MakeSegment
%{
%}

class GC_MakeSegment
{
 public:
  GC_MakeSegment(const gp_Pnt& P1,const gp_Pnt& P2);
  const Handle_Geom_TrimmedCurve& Value() const;
  Standard_Boolean IsDone() const;
};
