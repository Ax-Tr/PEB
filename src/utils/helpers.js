// Utility helpers for PEB Application

export const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2, 9);
};

export const formatCurrency = (amount) => {
  return '₹' + Number(amount).toLocaleString('en-IN', {
    minimumFractionDigits: 0,
    maximumFractionDigits: 2,
  });
};

export const formatDate = (dateString) => {
  const date = new Date(dateString);
  return date.toLocaleDateString('en-IN', {
    day: '2-digit',
    month: 'short',
    year: 'numeric',
  });
};

export const getGreeting = () => {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good Morning';
  if (hour < 17) return 'Good Afternoon';
  return 'Good Evening';
};

export const calculateNetSalary = (gross, lop, deductions, incentives) => {
  const lopDays = Number(lop) || 0;
  const deductionAmount = Number(deductions) || 0;
  const incentiveAmount = Number(incentives) || 0;
  const dailySalary = gross / 30;
  const lopAmount = dailySalary * lopDays;
  return gross - lopAmount - deductionAmount + incentiveAmount;
};

export const getStatusColor = (status) => {
  switch (status) {
    case 'completed': return '#00D9A6';
    case 'pending': return '#FFB547';
    case 'partial': return '#4DA6FF';
    case 'overdue': return '#FF6B6B';
    default: return '#A0A0C0';
  }
};

export const getTransactionIcon = (type) => {
  switch (type) {
    case 'receive': return 'arrow-down-left';
    case 'pay_employee': return 'users';
    case 'pay_vendor': return 'briefcase';
    default: return 'activity';
  }
};
